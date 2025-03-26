package moe.takochan.takotech.client.renderer.shader;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.OpenGlHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

@SideOnly(Side.CLIENT)
public class Framebuffer {
    public static Logger LOG = LogManager.getLogger();
    public static final List<Framebuffer> GLOBAL = new ArrayList<>();
    public int depthStencilBufferID;
    private int framebufferId;
    private int textureId;
    private int width;
    private int height;
    public int oldFrame;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        init();
        GLOBAL.add(this);
    }

    public void init() {
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
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
//        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL30.glFramebufferTexture2D(GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, textureId, 0);


        // 创建深度+模板缓冲（渲染缓冲对象）
        depthStencilBufferID = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthStencilBufferID);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        GL30.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, depthStencilBufferID);

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
        GL30.glDeleteRenderbuffers(depthStencilBufferID);
        GL11.glDeleteTextures(textureId);
    }

    public void renderToScreen() {
        // 备份所有状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);  // 备份属性状态
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();                     // 投影矩阵备份
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();                     // 模型视图矩阵备份

        // 获取旧视口
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        GL11.glGetInteger(GL_VIEWPORT, viewport);
        GL11.glViewport(0, 0, width, height);

        // 叠加到当前frame上
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); // 标准Alpha混合
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);

        // 立即模式渲染全屏四边形
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1000, 1000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(false);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex3f(0,height,999); // 左下
        GL11.glTexCoord2f(1, 0); GL11.glVertex3f(width,height,999); // 右下
        GL11.glTexCoord2f(1, 1); GL11.glVertex3f(width,0,999); // 右上
        GL11.glTexCoord2f(0, 1); GL11.glVertex3f(0,0,999); // 左上
        GL11.glEnd();

        // 恢复状态
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();  // 恢复属性状态

        // 恢复视口
        GL11.glViewport(viewport.get(0), viewport.get(1),
            viewport.get(2), viewport.get(3));
    }


    public int getFramebufferId() {
        return framebufferId;
    }

    public int getTextureId() {
        return textureId;
    }
}
