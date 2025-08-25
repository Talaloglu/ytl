# App Icons

The following directories contain placeholder references for app icons:

- `app/src/main/res/mipmap-hdpi/` - High density icons (72x72 dp)
- `app/src/main/res/mipmap-mdpi/` - Medium density icons (48x48 dp)  
- `app/src/main/res/mipmap-xhdpi/` - Extra high density icons (96x96 dp)
- `app/src/main/res/mipmap-xxhdpi/` - Extra extra high density icons (144x144 dp)
- `app/src/main/res/mipmap-xxxhdpi/` - Extra extra extra high density icons (192x192 dp)

## Required Icons

For a complete Android app, you need:
- `ic_launcher.png` - Main app icon
- `ic_launcher_round.png` - Rounded app icon (for devices that support it)

## How to Generate Icons

1. **Using Android Studio**:
   - Right-click on `app` in Project view
   - Select `New > Image Asset`
   - Choose "Launcher Icons (Adaptive and Legacy)"
   - Follow the wizard to generate all required sizes

2. **Using Online Tools**:
   - Use tools like [Icon Kitchen](https://icon.kitchen/)
   - Upload your icon design
   - Download the generated icon pack
   - Copy the appropriate files to their respective mipmap directories

3. **Manual Creation**:
   - Create icons in the sizes listed above
   - Save as PNG files with transparency
   - Place in the appropriate density folders

## Current Status

The AndroidManifest.xml references these icons:
- `@mipmap/ic_launcher`
- `@mipmap/ic_launcher_round`

**Note**: The app will not run properly without these icon files. Please generate them before building the project.