package polyllvm.visit;

import polyglot.ast.ClassBody;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.ParsedClassType;
import polyglot.util.Position;
import polyllvm.ast.PolyLLVMNodeFactory;
import polyllvm.types.PolyLLVMTypeSystem;
import polyllvm.util.Constants;

/** Declares a static field for each class to hold the class object for that class. */
public class DeclareClassObjects extends DesugarVisitor {

    public DeclareClassObjects(Job job, PolyLLVMTypeSystem ts, PolyLLVMNodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    protected ClassBody leaveClassBody(ParsedClassType ct, ClassBody cb) {
        Position pos = ct.position();

        String className = getClassObjectName(ct);
        Expr classNameExpr = tnf.StringLit(pos, className);

        ClassType classType = ts.Class();
        Expr init = tnf.StaticCall(pos, ts.Class(), ts.Class(), "forName", classNameExpr);

        Flags flags = Flags.NONE.Public().Static().Final();
        FieldDecl decl = tnf.FieldDecl(pos, ct, flags, classType, Constants.CLASS_OBJECT, init);

        return cb.members(concat(decl, cb.members()));
    }

    protected String getClassObjectName(ClassType t) {
        return t.outer() == null
                ? t.fullName()
                : getClassObjectName(t.outer()) + "$" + t.name();
    }
}
