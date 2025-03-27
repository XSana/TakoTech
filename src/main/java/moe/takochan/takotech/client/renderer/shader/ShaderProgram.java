package moe.takochan.takotech.client.renderer.shader;

import org.lwjgl.opengl.GL20;

import moe.takochan.takotech.TakoTechMod;

public class ShaderProgram extends com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram {

    public ShaderProgram(String domain, String vertShaderFilename, String fragShaderFilename) {
        super(domain, vertShaderFilename, fragShaderFilename);
    }

    public void setUniform(String name, float value) {
        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc >= 0) {
            GL20.glUniform1f(loc, value);
        } else {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
        }
    }

    public void setUniform(String name, float x, float y) {
        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc >= 0) {
            setUniform2f(name, x, y);
        } else {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
        }
    }

    public void setUniform(String name, int value) {
        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc >= 0) {
            GL20.glUniform1i(loc, value);
        } else {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
        }
    }

    public void setUniform2f(String name, float x, float y) {
        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc >= 0) {
            GL20.glUniform2f(loc, x, y);
        } else {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
        }
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc >= 0) {
            GL20.glUniform3f(loc, x, y, z);
        } else {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
        }
    }

}
