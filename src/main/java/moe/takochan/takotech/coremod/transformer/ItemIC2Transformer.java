package moe.takochan.takotech.coremod.transformer;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class ItemIC2Transformer implements IClassTransformer {

    /**
     * 字节码转换方法
     * <p>
     * 在类加载时调用，用于修改指定类的字节码
     *
     * @param name            类的原始名称
     * @param transformedName 类的转换后名称
     * @param basicClass      类的原始字节码
     * @return 修改后的字节码
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // 检查类名是否为 ic2.core.item.ItemIC2
        if (!"ic2.core.item.ItemIC2".equals(transformedName)) {
            // 如果不是目标类，直接返回原始字节码
            return basicClass;
        }
        // 如果不是目标类，直接返回原始字节码
        ClassReader reader = new ClassReader(basicClass);
        // 创建 ClassWriter 用于生成修改后的字节码
        ClassWriter writer = new ClassWriter(reader, 0);
        // 创建 ClassVisitor 用于访问和修改类的结构
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {

            /**
             * 访问类的方法
             * <p>
             * 用于拦截并修改目标方法
             *
             * @param access     方法的访问标志
             * @param name       方法名称
             * @param desc       方法描述符
             * @param signature  方法签名
             * @param exceptions 方法抛出的异常
             * @return MethodVisitor 用于进一步修改方法
             */
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                // 如果方法是构造方法，则返回自定义的 MethodVisitor
                if ("<init>".equals(name)) {
                    return new ItemIC2ConstructorTransformer(mv, access, name, desc);
                }
                return mv;
            }
        };
        // 使用 ClassVisitor 处理类的字节码
        reader.accept(visitor, 0);
        // 返回修改后的字节码
        return writer.toByteArray();
    }

    public static class ItemIC2ConstructorTransformer extends AdviceAdapter {

        /**
         * 构造函数
         *
         * @param mv     原始的 MethodVisitor
         * @param access 方法的访问标志
         * @param name   方法名称
         * @param desc   方法描述符
         */
        protected ItemIC2ConstructorTransformer(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        /**
         * 在方法入口处插入字节码
         * <p>
         * 检查 internalName 是否为 null，如果是则直接返回
         */
        @Override
        protected void onMethodEnter() {
            // 构造方法的局部变量：
            // index 0 -> this
            // index 1 -> InternalName 参数

            // 加载参数 internalName
            mv.visitVarInsn(ALOAD, 1);

            // 创建一个跳转标签，继续正常执行构造方法逻辑
            Label continueLabel = new Label();

            // 检查 internalName 是否为 null
            mv.visitJumpInsn(IFNONNULL, continueLabel);

            // 如果 internalName 是 null，直接返回（跳过后续构造逻辑）
            mv.visitInsn(RETURN);

            // 标记继续执行的地方
            mv.visitLabel(continueLabel);
        }
    }

}
