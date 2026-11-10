import os
import sys
from PIL import Image, ImageDraw

def generate_android_icons(src_image_path):
    res_dir = r"C:\Users\#Rajan Appa Gawade\.gemini\antigravity\scratch\MultibaggerBotApp\app\src\main\res"
    
    img = Image.open(src_image_path).convert("RGBA")
    w, h = img.size
    # crop to square if not square
    min_dim = min(w, h)
    left = (w - min_dim) // 2
    top = (h - min_dim) // 2
    img = img.crop((left, top, left + min_dim, top + min_dim))
    
    # 1. Generate adaptive foreground (512x512, with safe margins - centered ~340px)
    fg_canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    fg_resized = img.resize((360, 360), Image.Resampling.LANCZOS)
    
    # Create soft circular or rounded mask for the foreground inside adaptive canvas
    mask = Image.new("L", (360, 360), 0)
    draw_mask = ImageDraw.Draw(mask)
    draw_mask.rounded_rectangle([(0, 0), (360, 360)], radius=70, fill=255)
    
    # Paste centered
    offset = ((512 - 360) // 2, (512 - 360) // 2)
    fg_canvas.paste(fg_resized, offset, mask=mask)
    
    fg_drawable_path = os.path.join(res_dir, "drawable", "ic_launcher_foreground_img.png")
    fg_canvas.save(fg_drawable_path, "PNG")
    print(f"Saved adaptive foreground to: {fg_drawable_path}")
    
    # 2. Densities for legacy launcher icons
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    
    for folder, size in densities.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Square icon with rounded corners
        square_icon = img.resize((size, size), Image.Resampling.LANCZOS)
        sq_mask = Image.new("L", (size, size), 0)
        sq_draw = ImageDraw.Draw(sq_mask)
        corner_radius = int(size * 0.22)
        sq_draw.rounded_rectangle([(0, 0), (size, size)], radius=corner_radius, fill=255)
        
        final_square = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        final_square.paste(square_icon, (0, 0), mask=sq_mask)
        sq_path = os.path.join(folder_path, "ic_launcher.png")
        final_square.save(sq_path, "PNG")
        
        # Round icon (circle)
        round_icon = img.resize((size, size), Image.Resampling.LANCZOS)
        round_mask = Image.new("L", (size, size), 0)
        round_draw = ImageDraw.Draw(round_mask)
        round_draw.ellipse([(0, 0), (size, size)], fill=255)
        
        final_round = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        final_round.paste(round_icon, (0, 0), mask=round_mask)
        round_path = os.path.join(folder_path, "ic_launcher_round.png")
        final_round.save(round_path, "PNG")
        
        print(f"Saved {folder} icons ({size}x{size})")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        generate_android_icons(sys.argv[1])
    else:
        print("Usage: python create_icons.py <image_path>")
