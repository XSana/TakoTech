package moe.takochan.takotech.client.renderer.shader;

import org.lwjgl.opengl.GL20;

import moe.takochan.takotech.TakoTechMod;

public class ShaderProgram extends com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram {

    public ShaderProgram(String domain, String vertShaderFilename, String fragShaderFilename) {
        super(domain, vertShaderFilename, fragShaderFilename);
    }

    public boolean setUniformWithInt(String name, int... args) {
        if (!this.isValid()) {
            TakoTechMod.LOG.warn("ShaderProgram is not valid.");
            return false;
        }

        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc == -1) {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
            return false;
        }

        switch (args.length) {
            case 1:
                GL20.glUniform1i(loc, args[0]);
                return true;
            case 2:
                GL20.glUniform2i(loc, args[0], args[1]);
                return true;
            case 3:
                GL20.glUniform3i(loc, args[0], args[1], args[2]);
                return true;
            case 4:
                GL20.glUniform4i(loc, args[0], args[1], args[2], args[3]);
                return true;
            default:
                TakoTechMod.LOG.warn(
                    "Invalid number of arguments ({}) for uniform '{}' in shader {}. Expected 1-4 values.",
                    args.length,
                    name,
                    this.getProgram());
                return false;
        }
    }

    public boolean setUniformWithFloat(String name, float... args) {
        if (!this.isValid()) {
            TakoTechMod.LOG.warn("ShaderProgram is not valid.");
            return false;
        }

        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc == -1) {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader {}", name, this.getProgram());
            return false;
        }

        switch (args.length) {
            case 1:
                GL20.glUniform1f(loc, args[0]);
                return true;
            case 2:
                GL20.glUniform2f(loc, args[0], args[1]);
                return true;
            case 3:
                GL20.glUniform3f(loc, args[0], args[1], args[2]);
                return true;
            case 4:
                GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]);
                return true;
            default:
                TakoTechMod.LOG.warn(
                    "Invalid number of arguments ({}) for uniform '{}' in shader {}. Expected 1-4 values.",
                    args.length,
                    name,
                    this.getProgram());
                return false;
        }
    }

    public boolean isValid() {
        return this.getProgram() != 0;
    }

}
