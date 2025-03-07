package moe.takochan.takotech.coremod.transformer;

import java.io.FileOutputStream;
import java.io.IOException;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

public class ItemIC2Transformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"ic2.core.item.ItemIC2".equals(transformedName)) {
            return basicClass;
        }
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                if ("<init>".equals(name)) {
                    return new ItemIC2ConstructorTransformer(mv, access, name, desc);
                }
                return mv;
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    public static class ItemIC2ConstructorTransformer extends AdviceAdapter {

        protected ItemIC2ConstructorTransformer(MethodVisitor mv, int access, String name, String desc) {
            super(Opcodes.ASM5, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            // 构造方法的局部变量：
            // index 0 -> this
            // index 1 -> InternalName 参数
            // 调用 internalName.name() 与 null 比较

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
