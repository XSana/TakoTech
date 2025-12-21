package moe.takochan.takotech.client.renderer.graphics.texture;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

public class Texture2D {

    private int id = 0;
    private int width = 0;
    private int height = 0;
    private int format = GL11.GL_RGBA;
    private int unit = GL13.GL_TEXTURE0;

    private static final Map<Integer, Integer> lastBoundTextures = new HashMap<>();

    public Texture2D(int width, int height, int internalFormat) {
        this.width = width;
        this.height = height;
        this.format = internalFormat;

        this.id = GL11.glGenTextures();
        bind(0);

        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            internalFormat,
            width,
            height,
            0,
            internalFormat,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null);
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        setDefaultParameters();

        unbind(0);
    }

    public static Texture2D fromResource(String domain, String filename, int unitIndex) {
        ResourceLocation location = new ResourceLocation(domain, filename);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(location);
        int id = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        Texture2D texture = new Texture2D();
        texture.id = id;
        texture.unit = GL13.GL_TEXTURE0 + unitIndex;
        return texture;
    }

    private Texture2D() {}

    public void bind(int unitIndex) {
        this.unit = GL13.GL_TEXTURE0 + unitIndex;
        GL13.glActiveTexture(unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        lastBoundTextures.put(unit, id);
    }

    public void unbind(int unitIndex) {
        int unitEnum = GL13.GL_TEXTURE0 + unitIndex;
        GL13.glActiveTexture(unitEnum);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        lastBoundTextures.put(unitEnum, 0);
    }

    public void upload(ByteBuffer buffer) {
        bind(getUnitIndex());
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL11.GL_UNSIGNED_BYTE, buffer);
        unbind(getUnitIndex());
    }

    public void setFilter(int min, int mag) {
        bind(getUnitIndex());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
        unbind(getUnitIndex());
    }

    public void setWrap(int s, int t) {
        bind(getUnitIndex());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, s);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, t);
        unbind(getUnitIndex());
    }

    public boolean isValid() {
        return id != 0;
    }

    public void delete() {
        if (id != 0) {
            GL11.glDeleteTextures(id);
            id = 0;
        }
    }

    public int getId() {
        return id;
    }

    public int getUnitIndex() {
        return unit - GL13.GL_TEXTURE0;
    }

    private void setDefaultParameters() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }
}
