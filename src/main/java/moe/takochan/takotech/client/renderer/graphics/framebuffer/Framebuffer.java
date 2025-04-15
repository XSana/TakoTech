package moe.takochan.takotech.client.renderer.graphics.framebuffer;

import java.nio.ByteBuffer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

@SideOnly(Side.CLIENT)
public class Framebuffer {

    private int framebufferId;
    private int textureId;
    private int depthBufferId;
    private final int width;
    private final int height;

    public Framebuffer(int width, int height) {
        this(width, height, false);
    }

    public Framebuffer(int width, int height, boolean useDepth) {
        this.width = width;
        this.height = height;
        init(useDepth);
    }

    public void init(boolean useDepth) {
        // 创建FBO
        framebufferId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);

        // 创建Texture
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);

        // 创建DepthBuffer
        if (useDepth) {
            depthBufferId = GL30.glGenRenderbuffers();
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBufferId);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);
            GL30.glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_RENDERBUFFER,
                depthBufferId);
        }

        if (GL30.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete!");
        }

        GL30.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void bind() {
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        GL11.glViewport(0, 0, width, height);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    public void unbindTexture() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void delete() {
        GL30.glDeleteFramebuffers(framebufferId);
        GL11.glDeleteTextures(textureId);
    }

    public int getFramebufferId() {
        return framebufferId;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
