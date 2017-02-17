package polyllvm.extension;

import static org.bytedeco.javacpp.LLVM.*;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyllvm.ast.PolyLLVMNodeFactory;
import polyllvm.ast.PseudoLLVM.Expressions.*;
import polyllvm.ast.PseudoLLVM.LLVMGlobalVarDeclaration;
import polyllvm.ast.PseudoLLVM.LLVMTypes.LLVMArrayType;
import polyllvm.ast.PseudoLLVM.LLVMTypes.LLVMStructureType;
import polyllvm.ast.PseudoLLVM.LLVMTypes.LLVMTypeNode;
import polyllvm.util.LLVMUtils;
import polyllvm.util.PolyLLVMMangler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.bytedeco.javacpp.LLVM.LLVMInt8Type;
import static polyllvm.ast.PseudoLLVM.Expressions.LLVMVariable.VarKind.GLOBAL;
import static polyllvm.ast.PseudoLLVM.LLVMGlobalVarDeclaration.CONSTANT;

/**
 * Creates LLVM IR for class objects.
 */
public final class ClassObjects {

    public ClassObjects() {}

    public static LLVMTypeRef classIdVarTypeRef() {
        return LLVMInt8Type();
    }

    public static LLVMTypeRef classIdVarPtrTypeRef() {
        return  LLVMUtils.ptrTypeRef(classIdVarTypeRef());
    }

    public static LLVMValueRef classIdDeclRef(LLVMModuleRef mod,
                                                       ReferenceType rt,
                                                       boolean extern) {
        LLVMValueRef global = LLVMUtils.getGlobal(mod, PolyLLVMMangler.classIdName(rt), classIdVarTypeRef());
        if(!extern){
            LLVMSetInitializer(global, LLVMConstInt(LLVMInt8Type(), 0, /*sign-extend*/ 0));
        }
        return global;
    }

    public static LLVMValueRef classIdVarRef(LLVMModuleRef mod, ReferenceType rt) {
        return LLVMUtils.getGlobal(mod, PolyLLVMMangler.classIdName(rt), classIdVarPtrTypeRef());
    }

    public static LLVMValueRef classObjRef(LLVMModuleRef mod, ReferenceType rt) {
        List<LLVMValueRef> classObjPtrOperands = new ArrayList<>();

        Type superType = rt;
        while (superType != null) {
            classObjPtrOperands.add(classIdVarRef(mod, superType.toReference()));
            superType = superType.toReference().superType();
        }

        rt.interfaces().stream().map(it -> classIdVarRef(mod, it))
                .forEach(classObjPtrOperands::add);

        LLVMValueRef classObjPtrs = LLVMUtils.buildConstArray(classIdVarPtrTypeRef(), classObjPtrOperands.toArray(new LLVMValueRef[1]));
        LLVMValueRef numSupertypes = LLVMConstInt(LLVMInt32Type(), countSupertypes(rt), /*sign-extend*/ 0);
        LLVMValueRef classObjStruct = LLVMUtils.buildConstStruct(numSupertypes, classObjPtrs);

        LLVMValueRef global = LLVMUtils.getGlobal(mod, PolyLLVMMangler.classObjName(rt), LLVMTypeOf(classObjStruct));
        LLVMSetExternallyInitialized(global, 0);
        LLVMSetInitializer(global, classObjStruct);

        return global;
    }



    public static LLVMTypeNode classIdVarType(PolyLLVMNodeFactory nf) {
        return nf.LLVMIntType(8);
    }

    public static LLVMTypeNode classIdVarPtrType(PolyLLVMNodeFactory nf) {
        return nf.LLVMPointerType(classIdVarType(nf));
    }

    public static LLVMVariable classIdVar(PolyLLVMNodeFactory nf, ReferenceType rt) {
        return nf.LLVMVariable(PolyLLVMMangler.classIdName(rt),
                               classIdVarPtrType(nf),
                               GLOBAL);
    }

    public static LLVMGlobalVarDeclaration classIdDecl(PolyLLVMNodeFactory nf,
                                                ReferenceType rt,
                                                boolean extern) {
        return nf.LLVMGlobalVarDeclaration(
                PolyLLVMMangler.classIdName(rt),
                extern,
                CONSTANT,
                classIdVarType(nf),
                null
        );
    }

    public static LLVMTypedOperand classIdTypedVar(PolyLLVMNodeFactory nf, ReferenceType rf) {
        LLVMVariable classIdVar = classIdVar(nf, rf);
        return nf.LLVMTypedOperand(classIdVar, classIdVar.typeNode());
    }

    /** Counts the supertypes for this reference type, including itself. */
    public static int countSupertypes(ReferenceType rt) {
        int ret = 0;
        for (Type s = rt; s != null; s = s.toReference().superType())
            ++ret;
        ret += rt.interfaces().size();
        return ret;
    }

    public static LLVMArrayType classObjArrType(PolyLLVMNodeFactory nf, ReferenceType rt) {
        return nf.LLVMArrayType(classIdVarPtrType(nf), countSupertypes(rt));
    }

    public static LLVMTypeRef classObjArrTypeRef(ReferenceType rt) {
        return LLVMArrayType(classIdVarPtrTypeRef(), countSupertypes(rt));
    }

    public static LLVMStructureType classObjType(PolyLLVMNodeFactory nf, ReferenceType rt) {
        return nf.LLVMStructureType(Arrays.asList(nf.LLVMIntType(32), classObjArrType(nf, rt)));
    }

    public static LLVMTypeRef classObjTypeRef(ReferenceType rt) {
        return LLVMUtils.structType(LLVMInt32Type(), classObjArrTypeRef(rt));
    }

    public static LLVMVariable classObjVar(PolyLLVMNodeFactory nf, ReferenceType rt) {
        return nf.LLVMVariable(PolyLLVMMangler.classObjName(rt), classObjType(nf, rt), GLOBAL);
    }

    public static LLVMTypedOperand classObjTypedVar(PolyLLVMNodeFactory nf, ReferenceType rt) {
        LLVMVariable classObjVar = classObjVar(nf, rt);
        return nf.LLVMTypedOperand(classObjVar, classObjVar.typeNode());
    }

    public static LLVMGlobalVarDeclaration classObj(PolyLLVMNodeFactory nf, ReferenceType rt) {
        List<LLVMTypedOperand> classObjPtrOperands = new ArrayList<>();

        Type superType = rt;
        while (superType != null) {
            classObjPtrOperands.add(classIdTypedVar(nf, superType.toReference()));
            superType = superType.toReference().superType();
        }

        rt.interfaces().stream().map(it -> classIdTypedVar(nf, it))
                                .forEach(classObjPtrOperands::add);

        LLVMArrayLiteral classObjPtrs =
                nf.LLVMArrayLiteral(classObjArrType(nf, rt), classObjPtrOperands);
        LLVMOperand numSupertypes =
                nf.LLVMIntLiteral(nf.LLVMIntType(32), countSupertypes(rt));
        LLVMStructLiteral classObjStruct = nf.LLVMStructLiteral(classObjType(nf, rt), Arrays.asList(
                nf.LLVMTypedOperand(numSupertypes, numSupertypes.typeNode()),
                nf.LLVMTypedOperand(classObjPtrs, classObjPtrs.typeNode())
        ));

        return nf.LLVMGlobalVarDeclaration(
                PolyLLVMMangler.classObjName(rt),
                /* extern */ false,
                CONSTANT,
                classObjStruct.typeNode(),
                classObjStruct
        );
    }
}
