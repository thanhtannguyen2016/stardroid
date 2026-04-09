import json
import urllib.request
import urllib.parse
from xml.etree import ElementTree

# Mapping from Stellarium IAU abbr to our IDs and colors
CFG = {
    "Ori": {"id": "orion", "color": "0x60F0E68C"},
    "Sco": {"id": "scorpius", "color": "0x60FF6347"},
    "Leo": {"id": "leo", "color": "0x60FFD700"},
    "UMa": {"id": "ursa_major", "color": "0x6087CEEB"},
    "UMi": {"id": "ursa_minor", "color": "0x6087CEEB"},
}

def main():
    print("Downloading Stellarium index.json...")
    url = "https://raw.githubusercontent.com/Stellarium/stellarium-skycultures/master/western/index.json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))

    # Extract lines and collect HIP IDs
    constellations_lines = {}
    unique_hips = set()

    for item in data.get('constellations', []):
        iau = item.get('iau')
        if iau in CFG:
            lines = item.get('lines', [])
            constellations_lines[iau] = lines
            for stroke in lines:
                for pt in stroke:
                    if isinstance(pt, int):
                        unique_hips.add(pt)
                    elif isinstance(pt, str) and pt.isdigit():
                        unique_hips.add(int(pt))

    print(f"Found {len(unique_hips)} unique HIP stars. Fetching coordinates from VizieR...")
    hip_coords = {}
    
    # VizieR API
    hips_str = ",".join(str(h) for h in unique_hips)
    vizier_url = f"http://vizier.cds.unistra.fr/viz-bin/ascu-tab?-source=I/239/hip_main&-out=HIP,RAdeg,DEdeg&-out.max=unlimited&HIP={hips_str}"
    
    req = urllib.request.Request(vizier_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        lines = response.read().decode('ascii', errors='ignore').splitlines()
        for line in lines:
            if not line.strip() or line.startswith('#') or line.startswith('-'): continue
            parts = [p.strip() for p in line.split('|')]
            if len(parts) >= 4:
                try:
                    hip = int(parts[1])
                    ra = float(parts[2])
                    dec = float(parts[3])
                    hip_coords[hip] = {"ra": ra, "dec": dec}
                except ValueError:
                    pass

    print(f"Fetched coordinates for {len(hip_coords)} stars.")

    # Generate JSON
    output_data = []
    for iau, cfg in CFG.items():
        strokes_data = []
        lines = constellations_lines.get(iau, [])
        for i, stroke in enumerate(lines):
            # Ignore style strings like "thin"
            vertices = []
            for pt in stroke:
                if isinstance(pt, int) or (isinstance(pt, str) and pt.isdigit()):
                    hip = int(pt)
                    if hip in hip_coords:
                        # Append the coordinate
                        vertices.append(hip_coords[hip])
            if len(vertices) >= 2:
                strokes_data.append({
                    "comment": f"stroke_{i}",
                    "vertices": vertices
                })
        
        output_data.append({
            "id": cfg["id"],
            "comment": f"{cfg['id'].capitalize()} based on Stellarium Open Source.",
            "color": cfg["color"],
            "line_width": 1.5,
            "strokes": strokes_data
        })

    out_file = r"F:\build\stardroid\app\src\main\assets\constellation_figures.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(output_data, f, indent=2)

    print(f"Successfully generated {out_file}")

if __name__ == "__main__":
    main()
