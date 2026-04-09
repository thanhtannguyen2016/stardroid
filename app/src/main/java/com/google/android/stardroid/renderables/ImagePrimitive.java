// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.stardroid.renderables;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;

import com.google.android.stardroid.math.CoordinateManipulationsKt;
import com.google.android.stardroid.math.Vector3;

/**
 *  A celestial object represented by an image, such as a planet or a
 *  galaxy.
 */
public class ImagePrimitive extends AbstractPrimitive {

  static Vector3 up = new Vector3(0.0f, 1.0f, 0.0f);


  // These two vectors, along with Source.xyz, determine the position of the
  // image object.  The corners are as follows
  //
  //  xyz-u+v   xyz+u+v
  //     +---------+     ^
  //     |   xyz   |     | v
  //     |    .    |     .
  //     |         |
  //     +---------+
  //  xyz-u-v    xyz+u-v
  //
  //          .--->
  //            u
  public float ux, uy, uz;
  public float vx, vy, vz;

  public Bitmap image;

  public boolean requiresBlending = false;

  /** When true, use additive blending (GL_ONE) for glow effects. */
  public boolean useAdditiveBlending = false;

  /** When true, flip U vector to mirror the image horizontally. */
  public boolean horizontalFlip = false;

  private float imageScale;
  private Resources resources;


  public ImagePrimitive(float ra, float dec, Resources res, int id) {
    this(ra, dec, res, id, up, 1.0f);
  }

  public ImagePrimitive(float ra, float dec, Resources res, int id, Vector3 upVec) {
    this(ra, dec, res, id, upVec, 1.0f);
  }

  public ImagePrimitive(float ra, float dec, Resources res, int id, Vector3 upVec,
                        float imageScale) {
    this(CoordinateManipulationsKt.getGeocentricCoords(ra, dec), res, id, upVec, imageScale);
  }

  public ImagePrimitive(Vector3 coords, Resources res, int id, Vector3 upVec,
                        float imageScale) {
    super(coords, Color.WHITE);
    this.imageScale = imageScale;

    // TODO(jpowell): We're never freeing this resource, so we leak it every
    // time we create a new ImagePrimitive and garbage collect an old one.
    // We need to make sure it gets freed.
    // We should also cache this so we don't have to keep reloading these
    // which is really slow and adds noticeable lag to the application when it
    // happens.
    this.resources = res;
    setUpVector(upVec);
    setImageId(id);
  }

  /**
   * Constructor using a Bitmap directly with center position and separate U/V scales.
   */
  public ImagePrimitive(Vector3 coords, Bitmap bitmap, Vector3 upVec,
                        float scaleU, float scaleV) {
    super(coords, Color.WHITE);
    this.imageScale = 1.0f;
    this.resources = null;
    this.image = bitmap;

    Vector3 p = this.getLocation();
    Vector3 u = p.times(upVec).normalizedCopy().unaryMinus();
    Vector3 v = u.times(p);

    v.timesAssign(scaleV);
    u.timesAssign(scaleU);

    ux = u.x; uy = u.y; uz = u.z;
    vx = v.x; vy = v.y; vz = v.z;
  }

  /**
   * Anchor-based constructor: position an image by pinning 3 pixel locations
   * to 3 star positions (RA/Dec). This computes an affine transformation so
   * that the artwork features align precisely with the actual stars.
   *
   * @param bitmap The artwork bitmap
   * @param imgW   Reference image width (for normalizing pixel coords)
   * @param imgH   Reference image height
   * @param px1,py1 Pixel position of anchor 1 on the image
   * @param star1   3D geocentric coords of anchor star 1
   * @param px2,py2 Pixel position of anchor 2 on the image
   * @param star2   3D geocentric coords of anchor star 2
   * @param px3,py3 Pixel position of anchor 3 on the image
   * @param star3   3D geocentric coords of anchor star 3
   */
  public ImagePrimitive(Bitmap bitmap,
                        float imgW, float imgH,
                        float px1, float py1, Vector3 star1,
                        float px2, float py2, Vector3 star2,
                        float px3, float py3, Vector3 star3) {
    super(new Vector3(0, 0, 0), Color.WHITE);
    this.imageScale = 1.0f;
    this.resources = null;
    this.image = bitmap;
    this.requiresBlending = true;
    this.useAdditiveBlending = true;

    // Convert pixel coords to texture coords in [-1, 1] range
    // Texture mapping: (0,0) = upper-left, (1,1) = lower-right
    // Quad space: center + a*U + b*V where a,b in [-1,1]
    // Mapping: a = 2*(px/imgW) - 1, b = 1 - 2*(py/imgH)
    float a1 = 2.0f * (px1 / imgW) - 1.0f;
    float b1 = 1.0f - 2.0f * (py1 / imgH);
    float a2 = 2.0f * (px2 / imgW) - 1.0f;
    float b2 = 1.0f - 2.0f * (py2 / imgH);
    float a3 = 2.0f * (px3 / imgW) - 1.0f;
    float b3 = 1.0f - 2.0f * (py3 / imgH);

    // Solve for U and V vectors from the system:
    //   star1 = center + a1*U + b1*V
    //   star2 = center + a2*U + b2*V
    //   star3 = center + a3*U + b3*V
    //
    // Subtracting equations:
    //   star1 - star2 = (a1-a2)*U + (b1-b2)*V
    //   star1 - star3 = (a1-a3)*U + (b1-b3)*V

    float da12 = a1 - a2;
    float db12 = b1 - b2;
    float da13 = a1 - a3;
    float db13 = b1 - b3;

    // Determinant
    float det = da12 * db13 - da13 * db12;

    if (Math.abs(det) < 1e-8f) {
      // Degenerate case - anchors are collinear, fall back to simple positioning
      android.util.Log.e("ImagePrimitive", "Degenerate anchor points (collinear)");
      this.setLocation(star1);
      ux = 0.01f; uy = 0; uz = 0;
      vx = 0; vy = 0.01f; vz = 0;
      return;
    }

    float invDet = 1.0f / det;

    // Differences of star positions
    Vector3 d12 = star1.minus(star2);
    Vector3 d13 = star1.minus(star3);

    // Solve for U: U = invDet * (db13 * d12 - db12 * d13)
    Vector3 uVec = d12.times(db13).plus(d13.times(-db12));
    uVec.timesAssign(invDet);

    // Solve for V: V = invDet * (-da13 * d12 + da12 * d13)
    Vector3 vVec = d12.times(-da13).plus(d13.times(da12));
    vVec.timesAssign(invDet);

    // Solve for center: center = star1 - a1*U - b1*V
    Vector3 center = star1.minus(uVec.times(a1)).minus(vVec.times(b1));

    this.setLocation(center);
    ux = uVec.x; uy = uVec.y; uz = uVec.z;
    vx = vVec.x; vy = vVec.y; vz = vVec.z;
  }

  public void setImageId(int imageId) {
    Options opts = new Options();
    opts.inScaled = false;

    Bitmap newImage = BitmapFactory.decodeResource(resources, imageId, opts);
    if (newImage == null) {
      android.util.Log.e("ImagePrimitive", "Could not decode image " + imageId);
      return;
    }
    this.image = newImage;
  }

  public Bitmap getImage() {
    return image;
  }

  public float[] getHorizontalCorner() {
    return new float[] {ux, uy, uz};
  }

  public float[] getVerticalCorner() {
    return new float[] {vx, vy, vz};
  }

  public boolean requiresBlending() {
    return requiresBlending || useAdditiveBlending;
  }

  protected Resources getResources() {
    return resources;
  }

  public void setUpVector(Vector3 upVec) {
    Vector3 p = this.getLocation();
    Vector3 u = p.times(upVec).normalizedCopy().unaryMinus();
    Vector3 v = u.times(p);

    v.timesAssign(imageScale);
    u.timesAssign(imageScale);

    // TODO(serafini): Can we replace these with a float[]?
    ux = u.x;
    uy = u.y;
    uz = u.z;

    vx = v.x;
    vy = v.y;
    vz = v.z;
  }
}
