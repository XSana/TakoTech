package moe.takochan.takotech.client.renderer.shader;

import moe.takochan.takotech.TakoTechMod;
import org.lwjgl.opengl.GL20;

/**
 * ShaderProgram 是对 GTNHLib 中的 ShaderProgram 的扩展， 提供更简洁的 uniform 设置接口（支持 int 和 float 类型）。
 */
public class ShaderProgram extends com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram {

    /**
     * 构造函数。
     *
     * @param domain             资源域
     * @param vertShaderFilename 顶点着色器文件名
     * @param fragShaderFilename 片元着色器文件名
     */
    public ShaderProgram(String domain, String vertShaderFilename, String fragShaderFilename) {
        super(domain, vertShaderFilename, fragShaderFilename);
    }

    /**
     * 设置 int 类型的 uniform 变量，支持 1~4 个参数。
     *
     * @param name uniform 名称
     * @param args 1~4 个 int 参数
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setUniformInt(String name, int... args) {
        if (args.length < 1 || args.length > 4) {
            TakoTechMod.LOG.warn(
                "Invalid number of int arguments ({}) for uniform '{}'. Expected 1~4.",
                args.length,
                name);
            return false;
        }

        int loc = getUniformLocation(name);
        if (loc == -1) return false;

        switch (args.length) {
            case 1:
                GL20.glUniform1i(loc, args[0]);
                break;
            case 2:
                GL20.glUniform2i(loc, args[0], args[1]);
                break;
            case 3:
                GL20.glUniform3i(loc, args[0], args[1], args[2]);
                break;
            case 4:
                GL20.glUniform4i(loc, args[0], args[1], args[2], args[3]);
                break;
        }
        return true;
    }

    /**
     * 设置 float 类型的 uniform 变量，支持 1~4 个参数。
     *
     * @param name uniform 名称
     * @param args 1~4 个 float 参数
     * @return 设置成功返回 true，否则返回 false
     */
    public boolean setUniformFloat(String name, float... args) {
        if (args.length < 1 || args.length > 4) {
            TakoTechMod.LOG.warn(
                "Invalid number of float arguments ({}) for uniform '{}'. Expected 1~4.",
                args.length,
                name);
            return false;
        }

        int loc = getUniformLocation(name);
        if (loc == -1) return false;

        switch (args.length) {
            case 1:
                GL20.glUniform1f(loc, args[0]);
                break;
            case 2:
                GL20.glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                GL20.glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                GL20.glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
        return true;
    }

    /**
     * 获取 uniform 的 location，带有效性检查和日志输出。
     *
     * @param name uniform 变量名
     * @return 若有效返回 location，否则返回 -1
     */
    @Override
    public int getUniformLocation(String name) {
        if (!this.isValid()) {
            TakoTechMod.LOG.warn("ShaderProgram is not valid (program ID = 0).");
            return -1;
        }

        int loc = GL20.glGetUniformLocation(this.getProgram(), name);
        if (loc == -1) {
            TakoTechMod.LOG.warn("Uniform '{}' not found in shader program (ID = {}).", name, this.getProgram());
        }
        return loc;
    }

    /**
     * 判断当前 shader program 是否有效。
     *
     * @return 若有效则返回 true，否则返回 false
     */
    public boolean isValid() {
        return this.getProgram() != 0;
    }
}
