package cd4017be.rs_ctr.circuit.editor;

import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.SIPUSH;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import cd4017be.rscpl.compile.Context;
import cd4017be.rscpl.graph.Operator;
import it.unimi.dsi.fastutil.chars.Char2ObjectArrayMap;

/**
 * @author CD4017BE
 */
public class Code {

	final String desc;
	final byte[] bytecode;
	public final Type result;

	/**
	 * byte array version of {@link #OpCode(Type, String, int[])}
	 * @param desc code description
	 * @param code the {@link Opcodes}
	 */
	public Code(Type result, String desc, byte[] bytecode) {
		this.result = result;
		this.desc = desc;
		this.bytecode = bytecode;
	}

	/**
	 * <b>default local variables:</b><br>
	 * <b>m</b> int modified<br>
	 * <b>p</b> int[] ioBuffer<br>
	 * <b>t</b> __ this<dl>
	 * 
	 * <b>description patterns:</b><br>
	 * <b>0</b> - <b>9</b> numeric parameter n for following operation<br>
	 * <b>xn</b> enable hex mode for numeric parameter n<br>
	 * <b>Tv</b> define local variable v (single char) of type T<br>
	 * <b>Vv</b> release local variable v<br>
	 * <b>n+v</b> increment local variable v by n<br>
	 * <b>%v</b> instruction on local variable v<br>
	 * <b>n></b> evaluate input n<br>
	 * <b>*</b> single basic instruction<br>
	 * <b>n*</b> n basic instructions<br>
	 * <b>n#</b> instruction with numeric parameter<br>
	 * <b>n$</b> load constant with index n<br>
	 * <b>&T</b> instruction operating on type T<br>
	 * <b>|l</b> define jump label with name l (single char)<br>
	 * <b>!l</b> jump instruction to l<br>
	 * <b>n?m:dli...</b> table switch starting at number m with default label d and n jump labels li for m+i<br>
	 * <b>n?di:li...</b> lookup switch with default label d and n jump labels i:li where i is the key value and li is the label name<br>
	 * <b>=O:N T</b> field instruction with owner O, type T and name N<br>
	 * <b>;O:ND</b> method instruction with owner O, name N and descriptor D
	 * @param desc code description using above patterns
	 * @param code the {@link Opcodes}
	 */
	public Code(Type result, String desc, int... code) {
		this.result = result;
		this.desc = desc;
		byte[] bytecode = new byte[code.length];
		for (int i = bytecode.length - 1; i >= 0; i--)
			bytecode[i] = (byte)code[i];
		this.bytecode = bytecode;
	}

	/**
	 * compile the gate.
	 * This typically involves calling {@link #compile()} recursively on the input operands.
	 * @param context the method context
	 * @return list of instructions needed to add the gate's result on top of the Java operand stack.
	 */
	public InsnList compile(Context context, Operator[] in, Object... param) {
		InsnList list = new InsnList();
		Char2ObjectArrayMap<LabelNode> labels = new Char2ObjectArrayMap<>();
		Char2ObjectArrayMap<Local> locals = new Char2ObjectArrayMap<>();
		locals.put('m', new Local(Type.INT_TYPE, Context.DIRTY_IDX));
		locals.put('p', new Local(Type.getType("[I"), Context.IO_IDX));
		locals.put('t', new Local(Type.getObjectType(context.compiler.C_THIS), Context.THIS_IDX));
		int bi = 0, v = 0;
		boolean hex = false;
		char[] code = desc.toCharArray();
		for (int i = 0; i < code.length; i++) {
			char c = code[i];
			switch(c) {
			case 'I': case 'F': case 'J': case 'D': case 'L': {
				int j = parseDescriptor(code, i) + i;
				Type t = Type.getType(new String(code, i, j));
				c = code[i = j];
				Local l = locals.get(c);
				if (l != null) context.releaseLocal(l.idx, l.type);
				locals.put(c, new Local(t, context.newLocal(t)));
			}	break;
			case 'V': {
				Local l = locals.remove(code[++i]);
				context.releaseLocal(l.idx, l.type);
			}	break;
			case '+':
				list.add(new IincInsnNode(locals.get(code[++i]).idx, v));
				break;
			case '%':
				list.add(new VarInsnNode(bytecode[bi++] & 0xff, locals.get(code[++i]).idx));
				break;
			case '>':
				list.add(in[v].compile(context));
				break;
			case '*':
				do list.add(new InsnNode(bytecode[bi++] & 0xff));
				while(--v >= 0);
				break;
			case '#':
				list.add(new IntInsnNode(bytecode[bi++] & 0xff, v));
				break;
			case '$': {
				Object o = param[v];
				if (o instanceof Integer) {
					v = (Integer)o;
					if (v >= -1 && v <= 5)
						list.add(new InsnNode(ICONST_0 + v));
					else if (v >= Byte.MIN_VALUE && v < Byte.MAX_VALUE)
						list.add(new IntInsnNode(BIPUSH, v));
					else if (v >= Short.MIN_VALUE && v < Short.MAX_VALUE)
						list.add(new IntInsnNode(SIPUSH, v));
					else list.add(new LdcInsnNode(o));
				} else list.add(new LdcInsnNode(o));
			}	break;
			case '&': {
				int j = parseDescriptor(code, ++i);
				list.add(new TypeInsnNode(bytecode[bi++] & 0xff, new String(code, i, j)));
				i += j - 1;
			}	break;
			case '|':
				list.add(label(labels, code[++i]));
				break;
			case '!':
				list.add(new JumpInsnNode(bytecode[bi++] & 0xff, label(labels, code[++i])));
				break;
			case '?':
				c = code[++i];
				if (c >= '0' && c <= '9') {
					int[] n = parseNumber(code, i);
					int min = n[1];
					i = n[0];
					if ((c = code[i]) == ':') c = code[++i];
					LabelNode dflt = label(labels, c);
					LabelNode[] lbls = new LabelNode[v];
					for (int j = 0; j < v; j++)
						lbls[j] = label(labels, code[++i]);
					list.add(new TableSwitchInsnNode(min, min + v, dflt, lbls));
				} else {
					LabelNode dflt = label(labels, c);
					int[] keys = new int[v];
					LabelNode[] lbls = new LabelNode[v];
					for (int j = 0; j < v; j++) {
						int[] n = parseNumber(code, i + 1);
						i = n[0];
						keys[j] = n[1];
						if ((c = code[i]) == ':') c = code[++i];
						lbls[j] = label(labels, c);
					}
					list.add(new LookupSwitchInsnNode(dflt, keys, lbls));
				}
				break;
			case '=': case ';': {
				boolean field = c == '=';
				int j = parseUntil(code, ++i, ':');
				String owner = new String(code, i, j);
				if (owner.isEmpty()) owner = context.compiler.C_THIS;
				j = parseUntil(code, i = j + 1, field ? ' ' : '(');
				String name = new String(code, i, j);
				i = j; j++;
				if (!field) {
					while(code[j] != ')')
						j += parseDescriptor(code, j);
				}
				j += parseDescriptor(code, ++j);
				String desc = new String(code, i, j);
				if (field) list.add(new FieldInsnNode(bytecode[bi++] & 0xff, owner, name, desc));
				else {
					int ins = bytecode[bi++] & 0xff;
					list.add(new MethodInsnNode(ins, owner, name, desc, ins == Opcodes.INVOKEINTERFACE));
				}
				i = j;
			}	break;
			case 'x':
				hex = true;
				continue;
			default:
				int n = digit(c, hex);
				if (n < 0) break;
				if (hex) v = v << 4 | n;
				else v = v * 10 + n;
				continue;
			}
			v = 0;
			hex = false;
		}
		for (Local l : locals.values())
			context.releaseLocal(l.idx, l.type);
		return list;
	}

	static int digit(char c, boolean hex) {
		if (c >= '0' && c <= '9') return c - '0';
		if (!hex) return -1;
		if (c >= 'a' && c <= 'f') return c - 'a' + 10;
		if (c >= 'A' && c <= 'F') return c - 'A' + 10;
		return -1;
	}

	static int[] parseNumber(char[] code, int ofs) {
		boolean hex = false;
		int v = 0;
		while(ofs < code.length) {
			char c = code[ofs++];
			if (c == 'x') hex = true;
			int n = digit(c, hex);
			if (n < 0) break;
			if (hex) v = v << 4 | n;
			else v = v * 10 + n;
		}
		return new int[] {ofs, v};
	}

	static LabelNode label(Char2ObjectArrayMap<LabelNode> labels, char k) {
		LabelNode l = labels.get(k);
		if (l == null) labels.put(k, l = new LabelNode());
		return l;
	}

	static int parseDescriptor(char[] code, int ofs) {
		char c = code[ofs];
		if (c == '[')
			return parseDescriptor(code, ofs + 1) + 1;
		if (c == 'L') {
			int n = 2;
			while(code[++ofs] != ';') n++;
			return n;
		}
		return 1;
	}

	static int parseUntil(char[] code, int ofs, char term) {
		while(code[ofs++] != term);
		return ofs - 1;
	}

	static class Local {
		final int idx;
		final Type type;
		public Local(Type type, int idx) {
			this.idx = idx;
			this.type = type;
		}
	}

}
