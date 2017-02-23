package polyllvm.extension;

import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.ReferenceType;
import polyglot.util.SerialVersionUID;
import polyllvm.ast.PolyLLVMExt;
import polyllvm.ast.PolyLLVMNodeFactory;
import polyllvm.util.LLVMUtils;
import polyllvm.visit.PseudoLLVMTranslator;

import static org.bytedeco.javacpp.LLVM.*;

public class PolyLLVMInstanceofExt extends PolyLLVMExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node translatePseudoLLVM(PseudoLLVMTranslator v) {
        Instanceof n = (Instanceof) node();
        PolyLLVMNodeFactory nf = v.nodeFactory();
        LLVMValueRef obj =  v.getTranslation(n.expr());
        ReferenceType compareRt = n.compareType().type().toReference();
        LLVMValueRef compTypeIdVar = ClassObjects.classIdVarRef(v.mod, compareRt);
        LLVMTypeRef bytePtrType = LLVMUtils.ptrTypeRef(LLVMInt8Type());

        // Declared the class id variable for the compare type.
        LLVMValueRef compTypeIdDecl = ClassObjects.classIdDeclRef(v.mod, compareRt, /* extern */ true);
        System.out.println("class id: " + LLVMPrintValueToString(compTypeIdDecl).getString());

        // Cast obj to a byte pointer.
        LLVMValueRef objBitcast = LLVMBuildBitCast(v.builder, obj, bytePtrType, "cast_obj_byte_ptr");
        System.out.println("bitcast: " + LLVMPrintValueToString(objBitcast).getString());

        // Build call to native code.
        LLVMValueRef function = LLVMUtils.getFunction(v.mod, "instanceof",
                LLVMUtils.functionType(LLVMInt1Type(), bytePtrType, bytePtrType));
        System.out.println("instanceof function: " + LLVMPrintValueToString(function).getString());
        LLVMValueRef result = LLVMUtils.buildMethodCall(v.builder, function, objBitcast, compTypeIdVar);
        System.out.println("result: " + LLVMPrintValueToString(result).getString());

        v.addTranslation(n, result);
        return super.translatePseudoLLVM(v);
    }
}