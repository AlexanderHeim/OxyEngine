package OxyEngine.Core.Renderer.Buffer.Platform;

import OxyEngine.Core.Renderer.Buffer.FrameBuffer;
import OxyEngine.OxyEngine;

import java.nio.FloatBuffer;

import static OxyEngine.System.OxySystem.oxyAssert;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_2D_MULTISAMPLE;
import static org.lwjgl.opengl.GL32.glTexImage2DMultisample;
import static org.lwjgl.opengl.GL45.glCreateFramebuffers;
import static org.lwjgl.opengl.GL45.glCreateRenderbuffers;

public class OpenGLFrameBuffer extends FrameBuffer {

    OpenGLFrameBuffer(int width, int height) {
        super(width, height);
    }

    @Override
    public void load() {

        if (width <= 10 || height <= 10) {
            windowMinized = true;
            return;
        } else windowMinized = false;

        if (bufferId == 0) bufferId = glCreateFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, bufferId);

        int samples = OxyEngine.getAntialiasing().getLevel();

        colorAttachmentId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, colorAttachmentId);
        glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, GL_RGBA8, width, height, true);
        glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D_MULTISAMPLE, colorAttachmentId, 0);

        int rbo = glCreateRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorageMultisample(GL_RENDERBUFFER, samples, GL_DEPTH24_STENCIL8, width, height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        assert glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE : oxyAssert("Framebuffer is incomplete!");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        intermediateFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, intermediateFBO);
        colorAttachmentTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorAttachmentTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_FLOAT, (FloatBuffer) null); //GL_RGBA8 for standard
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttachmentTexture, 0);
        assert glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE : oxyAssert("Framebuffer is incomplete!");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void bind() {
        if (!windowMinized) {
            glBindFramebuffer(GL_FRAMEBUFFER, bufferId);
            glViewport(0, 0, width, height);
        }
    }

    @Override
    public void blit() {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, bufferId);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, intermediateFBO);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
    }

    public int getColorAttachmentTexture() {
        return colorAttachmentTexture;
    }

    @Override
    public void resize(float width, float height) {
        this.width = (int) width;
        this.height = (int) height;

        load(); //load it again, so that it has the new width/height values.
    }

    @Override
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(bufferId);
        glDeleteTextures(colorAttachmentId);
        glDeleteTextures(colorAttachmentTexture);
    }
}