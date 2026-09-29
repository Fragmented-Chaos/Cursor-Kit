package com.fragmentedchaos.cursorkit.client.render;

import com.fragmentedchaos.cursorkit.Constants;
import com.fragmentedchaos.cursorkit.cursor.model.CursorImage;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * A cursor image loaded from a source of our own (the mod classpath or the config directory).
 * <p>
 * Extending {@code ReloadableTexture} means the texture manager reloads and uploads it with every
 * resource reload, exactly like a pack texture.
 */
public final class CursorTexture extends ReloadableTexture {

    private final Identifier textureId;
    private final IoSupplier<InputStream> streamSupplier;
    private final CursorImage image;

    public CursorTexture(Identifier textureId, IoSupplier<InputStream> streamSupplier,
                         CursorImage image) {
        super(textureId);
        this.textureId = textureId;
        this.streamSupplier = streamSupplier;
        this.image = image;
    }

    @Override
    public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
        try (InputStream stream = this.streamSupplier.get()) {
            byte[] bytes = stream.readAllBytes();
            NativeImage decoded;
            if (CursorFile.isCursor(bytes)) {
                decoded = CursorFile.decode(bytes);
                if (decoded == null) {
                    throw new IOException("unsupported cursor image " + this.textureId);
                }
                CursorFile.Entry entry = CursorFile.largest(bytes);
                if (entry != null) {
                    CursorGeometry.recordHotspot(this.textureId, entry.hotspotX(), entry.hotspotY());
                }
            } else {
                decoded = NativeImage.read(new java.io.ByteArrayInputStream(bytes));
            }
            measure(decoded);
            // Never blurred: a cursor is meant to look like the system pointer, so it is drawn at a
            // whole multiple of its own pixels (see CursorRenderer) and sampled nearest neighbour.
            // Linear filtering turned high resolution cursors into a soft smudge.
            TextureMetadataSection metadata = new TextureMetadataSection(false, false,
                    MipmapStrategy.AUTO, TextureMetadataSection.DEFAULT_ALPHA_CUTOFF_BIAS);
            return new TextureContents(decoded, metadata);
        }
    }

    /**
     * Works out how large one frame really is, so images other than 16x16 work as well, and reports
     * anything that would make the cursor look misplaced.
     */
    private void measure(NativeImage decoded) {
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        int frameSize = Math.max(1, height);
        int frames = Math.max(1, width / frameSize);
        CursorGeometry.record(this.textureId, frameSize, frames);

        if (width % frameSize != 0) {
            Constants.LOG.warn("Cursor texture {} is {}x{}, which is not a whole number of {}px"
                            + " frames; only the first {} frame(s) are used",
                    this.textureId, width, height, frameSize, frames);
        } else if (frames != this.image.frames()) {
            Constants.LOG.warn("Cursor texture {} holds {} frame(s) of {}x{} but the JSON says {};"
                            + " the image wins",
                    this.textureId, frames, frameSize, frameSize, this.image.frames());
        }
        int hotspotX = CursorGeometry.of(this.textureId, this.image).hotspotX(this.image);
        int hotspotY = CursorGeometry.of(this.textureId, this.image).hotspotY(this.image);
        if (hotspotX >= frameSize || hotspotY >= frameSize) {
            Constants.LOG.warn("Cursor texture {} has hotspot {}x{} outside its {}x{} frame, so it"
                            + " will be drawn away from the pointer",
                    this.textureId, hotspotX, hotspotY, frameSize, frameSize);
        }
    }
}
