package pl.asie.environmentchecker;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ASMUtil {
    private ASMUtil() {

    }

    private static void addOpcodeNodes(InsnList list, List<AbstractInsnNode> target) {
        for (int i = 0; i < list.size(); i++) {
            AbstractInsnNode node = list.get(i);
            if (node instanceof FrameNode || node instanceof LabelNode || node instanceof LineNumberNode) {
                continue;
            }
            target.add(node);
        }
    }

    public static boolean equalsOpcodeWise(InsnList list1, InsnList list2) {
        List<AbstractInsnNode> nodes1 = new ArrayList<>();
        List<AbstractInsnNode> nodes2 = new ArrayList<>();

        addOpcodeNodes(list1, nodes1);
        addOpcodeNodes(list2, nodes2);

        if (nodes1.size() != nodes2.size()) return false;

        for (int i = 0; i < nodes1.size(); i++) {
            if (!equalsOpcodeWise(nodes1.get(i), nodes2.get(i))) return false;
        }

        return true;
    }

    public static boolean equals(Label label1, Label label2) {
        // TODO
        return true;
        // return label1.getOffset() == label2.getOffset();
    }

    public static boolean equals(List<LabelNode> labels1, List<LabelNode> labels2) {
        if (labels1.size() != labels2.size())
            return false;
        for (int i = 0; i < labels1.size(); i++) {
            if (!equals(labels1.get(i).getLabel(), labels2.get(i).getLabel())) {
                return false;
            }
        }
        return true;
    }

    public static boolean equalsOpcodeWise(AbstractInsnNode node1, AbstractInsnNode node2) {
        if (node1.getOpcode() != node2.getOpcode() || node1.getType() != node2.getType()) {
            return false;
        } else if (node1.getClass() != node2.getClass()) {
            System.out.println("ASMUtil.equals found class inequality for same opcode " + node1.getOpcode() + "! This may be a bug!");
            return false;
        } else {
            if (node1 instanceof FieldInsnNode) {
                return Objects.equals(((FieldInsnNode) node1).name, ((FieldInsnNode) node2).name)
                        && Objects.equals(((FieldInsnNode) node1).desc, ((FieldInsnNode) node2).desc)
                        && Objects.equals(((FieldInsnNode) node1).owner, ((FieldInsnNode) node2).owner);
            } else if (node1 instanceof FrameNode) {
                // opcode-wise, we ignore frames
                return true;
            } else if (node1 instanceof IincInsnNode) {
                return ((IincInsnNode) node1).incr == ((IincInsnNode) node2).incr
                        && ((IincInsnNode) node1).var == ((IincInsnNode) node2).var;
            } else if (node1 instanceof InsnNode) {
                // same opcode
                return true;
            } else if (node1 instanceof IntInsnNode) {
                return ((IntInsnNode) node1).operand == ((IntInsnNode) node2).operand;
            } else if (node1 instanceof InvokeDynamicInsnNode) {
                return ((InvokeDynamicInsnNode) node1).bsm.equals(((InvokeDynamicInsnNode) node2).bsm)
                        && Objects.equals(((InvokeDynamicInsnNode) node1).name, ((InvokeDynamicInsnNode) node2).name)
                        && Objects.equals(((InvokeDynamicInsnNode) node1).desc, ((InvokeDynamicInsnNode) node2).desc)
                        && Arrays.equals(((InvokeDynamicInsnNode) node1).bsmArgs, ((InvokeDynamicInsnNode) node2).bsmArgs);
            } else if (node1 instanceof JumpInsnNode) {
                return equals(((JumpInsnNode) node1).label.getLabel(), ((JumpInsnNode) node2).label.getLabel());
            } else if (node1 instanceof LdcInsnNode) {
                return Objects.equals(((LdcInsnNode) node1).cst, ((LdcInsnNode) node2).cst);
            } else if (node1 instanceof LineNumberNode) {
                return true;
            } else if (node1 instanceof LookupSwitchInsnNode) {
                if (((LookupSwitchInsnNode) node1).keys.equals(((LookupSwitchInsnNode) node2).keys)
                        && equals(((LookupSwitchInsnNode) node1).dflt.getLabel(), ((LookupSwitchInsnNode) node1).dflt.getLabel())) {

                    List<LabelNode> labels1 = ((LookupSwitchInsnNode) node1).labels;
                    List<LabelNode> labels2 = ((LookupSwitchInsnNode) node2).labels;
                    return equals(labels1, labels2);
                }

                return false;
            } else if (node1 instanceof MethodInsnNode) {
                return Objects.equals(((MethodInsnNode) node1).desc, ((MethodInsnNode) node2).desc)
                        && ((MethodInsnNode) node1).itf == ((MethodInsnNode) node2).itf
                        && Objects.equals(((MethodInsnNode) node1).name, ((MethodInsnNode) node2).name)
                        && Objects.equals(((MethodInsnNode) node1).owner, ((MethodInsnNode) node2).owner);
            } else if (node1 instanceof MultiANewArrayInsnNode) {
                return Objects.equals(((MultiANewArrayInsnNode) node1).desc, ((MultiANewArrayInsnNode) node2).desc)
                        && ((MultiANewArrayInsnNode) node1).dims == ((MultiANewArrayInsnNode) node2).dims;
            } else if (node1 instanceof TableSwitchInsnNode) {
                if (((TableSwitchInsnNode) node1).max == ((TableSwitchInsnNode) node2).max
                    && ((TableSwitchInsnNode) node1).min == ((TableSwitchInsnNode) node2).min
                    && equals(((TableSwitchInsnNode) node1).dflt.getLabel(), ((TableSwitchInsnNode) node2).dflt.getLabel())) {

                    List<LabelNode> labels1 = ((TableSwitchInsnNode) node1).labels;
                    List<LabelNode> labels2 = ((TableSwitchInsnNode) node2).labels;
                    return equals(labels1, labels2);
                }

                return false;
            } else if (node1 instanceof TypeInsnNode) {
                return Objects.equals(((TypeInsnNode) node1).desc, ((TypeInsnNode) node2).desc);
            } else if (node1 instanceof VarInsnNode) {
                return ((VarInsnNode) node1).var == ((VarInsnNode) node2).var;
            } else {
                System.out.println("ASMUtil.equals found unknown AbstractInsnNode: " + node1.getClass().getName() + "! This may be a bug!");
                return true;
            }
        }
    }
}
