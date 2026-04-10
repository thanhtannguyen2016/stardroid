import json
import urllib.request
import os

CFG = {
    "Ori": {"id": "orion", "color": "0x80F0E68C"},
    "Sco": {"id": "scorpius", "color": "0x80FF6347"},
    "Leo": {"id": "leo", "color": "0x80FFD700"},
    "UMa": {"id": "ursa_major", "color": "0x8087CEEB"},
    "UMi": {"id": "ursa_minor", "color": "0x8087CEEB"},
    "Tau": {"id": "taurus", "color": "0x80F08080"},
    "Gem": {"id": "gemini", "color": "0x8098FB98"},
    "Vir": {"id": "virgo", "color": "0x80DDA0DD"},
    "Peg": {"id": "pegasus", "color": "0x80B0C4DE"},
    "Cyg": {"id": "cygnus", "color": "0x80ADD8E6"},
    "Cas": {"id": "cassiopeia", "color": "0x80FFB6C1"},
    "And": {"id": "andromeda", "color": "0x80E6E6FA"},
    "CMa": {"id": "canis_major", "color": "0x80FFFFE0"},
    "Boo": {"id": "bootes", "color": "0x80FFA07A"},
    "Sgr": {"id": "sagittarius", "color": "0x8020B2AA"}
}

def main():
    print("Downloading Stellarium index.json...")
    url = "https://raw.githubusercontent.com/Stellarium/stellarium-skycultures/master/western/index.json"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib.request.urlopen(req)
    data = json.loads(response.read().decode('utf-8'))

    unique_hips = set()
    selected_constellations = []

    # Read the JSON from Stellarium for the 15 requested constellations
    for item in data.get('constellations', []):
        iau = item.get('iau')
        if iau in CFG:
            selected_constellations.append(item)
            
            # Fetch HIPs from lines
            lines = item.get('lines', [])
            for stroke in lines:
                for pt in stroke:
                    if isinstance(pt, int):
                        unique_hips.add(pt)
                    elif isinstance(pt, str) and pt.isdigit():
                        unique_hips.add(int(pt))
                        
            # Fetch HIPs from anchors in image if present
            image_obj = item.get('image', {})
            if image_obj:
                for anchor in image_obj.get('anchors', []):
                    hip = anchor.get('hip')
                    if hip:
                        unique_hips.add(int(hip))

    print(f"Found {len(unique_hips)} unique HIP stars. Fetching coordinates from VizieR...")
    hip_coords = {}
    
    # VizieR API has limits; breaking queries into chunks of 100 HIPs
    hips_list = list(unique_hips)
    chunk_size = 100
    for i in range(0, len(hips_list), chunk_size):
        chunk = hips_list[i:i+chunk_size]
        hips_str = ",".join(str(h) for h in chunk)
        vizier_url = f"http://vizier.cds.unistra.fr/viz-bin/asu-tsv?-source=I/239/hip_main&-out=HIP,_RA.icrs,_DE.icrs&-out.max=unlimited&HIP={hips_str}"
        print(f"Fetching chunk {i//chunk_size + 1} from VizieR...")
        req = urllib.request.Request(vizier_url, headers={'User-Agent': 'Mozilla/5.0'})
        response = urllib.request.urlopen(req)
        lines = response.read().decode('ascii', errors='ignore').splitlines()
        for line in lines:
            if not line.strip() or line.startswith('#') or line.startswith('-'): continue
            # Handle empty tab fields
            parts = [p.strip() for p in line.split('\t')]
            if len(parts) >= 3:
                try:
                    hip = int(parts[0])
                    ra = float(parts[1])
                    dec = float(parts[2])
                    hip_coords[hip] = {"ra": ra, "dec": dec}
                except ValueError:
                    pass

    print(f"Fetched coordinates for {len(hip_coords)} stars. Generating files...")

    # Generate JSONs and the Download Bat File
    figures_data = []
    art_data = []
    art_script_lines = []
    
    art_script_lines.append("@echo off")
    art_script_lines.append("echo Downloading 15 Constellation Artworks...")
    art_script_lines.append("set PS=\"C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe\"")
    art_script_lines.append("set BASE=https://raw.githubusercontent.com/Stellarium/stellarium-skycultures/master/western/illustrations")
    art_script_lines.append("set DST=F:\\build\\stardroid\\app\\src\\main\\assets\\constellation_artwork")
    
    for item in selected_constellations:
        iau = item.get('iau')
        cfg = CFG[iau]
        target_id = cfg['id']
        
        # 1. Figures Line Art
        strokes_data = []
        for i, stroke in enumerate(item.get('lines', [])):
            vertices = []
            for pt in stroke:
                if isinstance(pt, int) or (isinstance(pt, str) and pt.isdigit()):
                    hip = int(pt)
                    if hip in hip_coords:
                        vertices.append(hip_coords[hip])
            if len(vertices) >= 2:
                strokes_data.append({"comment": f"stroke_{i}", "vertices": vertices})
        
        figures_data.append({
            "id": target_id,
            "comment": f"{target_id} figures from Stellarium",
            "color": cfg["color"],
            "line_width": 1.5,
            "strokes": strokes_data
        })
        
        # 2. Artwork Overlay
        img_obj = item.get('image', {})
        if img_obj:
            img_file = img_obj.get('file', '')
            img_filename = img_file.split('/')[-1] if '/' in img_file else img_file
            if not img_filename:
                # Provide a sensible default fallback image name
                img_filename = f"{target_id}.webp"
            
            size = img_obj.get('size', [512, 512])
            anchors = []
            for anchor in img_obj.get('anchors', []):
                hip = anchor.get('hip')
                pos = anchor.get('pos', [0,0])
                if hip in hip_coords:
                    anchors.append({
                        "px": float(pos[0]),
                        "py": float(pos[1]),
                        "ra": float(hip_coords[hip]["ra"]),
                        "dec": float(hip_coords[hip]["dec"])
                    })
            if len(anchors) >= 3:
                art_data.append({
                    "id": target_id,
                    "image": f"constellation_artwork/{img_filename}",
                    "image_size": size,
                    "anchors": anchors
                })
                # Add to script commands
                art_script_lines.append(f"echo.")
                art_script_lines.append(f"echo Downloading {target_id}...")
                art_script_lines.append(f"%PS% -Command \"Invoke-WebRequest -Uri '%BASE%/{img_filename}' -OutFile '%DST%\\{img_filename}'\"")

    art_script_lines.append("echo.")
    art_script_lines.append("echo All backgrounds updated successfully!")
    art_script_lines.append("pause")

    # Write output JSON files
    base_dir = r"F:\build\stardroid\app\src\main\assets"
    
    with open(os.path.join(base_dir, "constellation_figures.json"), "w", encoding="utf-8") as f:
        json.dump(figures_data, f, indent=2)
        
    with open(os.path.join(base_dir, "constellation_art.json"), "w", encoding="utf-8") as f:
        json.dump(art_data, f, indent=2)
        
    # Write output downloader script
    bat_file = r"F:\build\stardroid\copy_artwork.bat"
    with open(bat_file, "w", encoding="utf-8") as f:
        f.write("\n".join(art_script_lines))

    print("Success! Wrote 15 constellations into JSON files and updated copy_artwork.bat with new artwork URLs!")

if __name__ == "__main__":
    main()
