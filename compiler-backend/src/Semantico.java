import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class Semantico implements Constants {

    private static final String INT_TYPE = "int64";
    private static final String FLOAT_TYPE = "float64";
    private static final String BOOLEAN_TYPE = "bool";
    private static final String STRING_TYPE = "string";
    private StringBuilder code;
    private Stack<String> typeStack = new Stack<>();
    private Stack<String> pilhaRotulos = new Stack<>();
    private String operadorRelacional;
    private List<String> tabelaSimbolos = new ArrayList<>();
    private List<String> listaIds = new ArrayList<>();
    private int rotuleCounter;

    /**
     * Executa as acoes recebidas de acordo com o codigo
     */
    public void executeAction(int action, Token token) throws SemanticError {
        System.out.println("Ação #" + action + ", Token: " + token);

        switch (action) {
            case 100:
                generateHeader();
                break;
            case 101:
                generateFooter();
                break;
            case 102:
                setEqualsListaIds();
                break;
            case 103:
                setExpressionValue();
                break;
            case 104:
                saveIdentifier(token);
                break;
            case 105:
                readIdentifier(token);
                break;
            case 106:
                writeConstant(token);
                break;
            case 107:
                breakLine();
                break;
            case 108:
                write();
                break;
            case 109:
                selection();
                break;
            case 110:
                selectCommandRotulos();
                break;
            case 111:
                deleteRotulo();
                break;
            case 112:
                createRotulo(token);
                break;
            case 114:
                afterExpressao(token);
                break;
            case 116:
                and();
                break;
            case 117:
                or();
                break;
            case 118:
                stackTrue();
                break;
            case 119:
                stackFalse();
                break;
            case 120:
                not();
                break;
            case 121:
                saveRelationalOperation(token);
                break;
            case 122:
                performRelationalOperation(token);
                break;
            case 123:
                add();
                break;
            case 124:
                sub();
                break;
            case 125:
                mul();
                break;
            case 126:
                div();
                break;
            case 127:
                identifier(token);
                break;
            case 128:
                stackInt(token);
                break;
            case 129:
                stackFloat(token);
                break;
            case 130:
                stackString(token);
                break;
            case 131:
                subtraction();
                break;
            default:
                System.out.println("Default...");
        }

    }

    private void setEqualsListaIds() throws SemanticError {
        for (final String id : listaIds) {
            final String type;

            final Optional<String> simbol = tabelaSimbolos.stream()
                    .filter(v -> v.equals(id))
                    .findFirst();

            if (simbol.isPresent()) {
                throw new SemanticError(id + " já declarado");
            }

            tabelaSimbolos.add(id);

            if (id.startsWith("i_")) {
                type = INT_TYPE;
            } else if (id.startsWith("f_")) {
                type = FLOAT_TYPE;
            } else if (id.startsWith("s_")) {
                type = STRING_TYPE;
            } else {
                type = BOOLEAN_TYPE;
            }

            code.append(String.format(".locals (%s %s)", type, id))
                    .append("\n");
        }

        listaIds.clear();
    }

    // TODO: 24/11/2024 refinar regra dessa acao
    private void setExpressionValue() throws SemanticError {

        String expressionType = typeStack.pop();

        if (expressionType.equals(INT_TYPE)) {
            code.append("conv.i8")
                    .append("\n");
        }

        for (final String id : this.listaIds) {

            // verificar na tabela de simbolos se o id foi realmente declarado
            boolean idDeclared = tabelaSimbolos.contains(id);
            if (!idDeclared) throw new SemanticError(id + "não declarado");

            // gerar codigo para carregar valor da expressao em id
            code.append("stloc")
                    .append(" ")
                    .append(id)
                    .append("\n");
        }
        listaIds.clear();
    }

    private void saveIdentifier(Token token) {
        this.listaIds.add(token.getLexeme());
    }

    private void readIdentifier(Token token) throws SemanticError { // ou writeIdentifier....?
        String id = token.getLexeme();

        // verificar se o id foi declarado
        boolean isIdDeclared = tabelaSimbolos.contains(id);
        if (!isIdDeclared) throw new SemanticError(id + " não declarado");


        if (id.startsWith("i_")) {
            code.append("call string [mscorlib]System.Console::ReadLine()")
                    .append("\n")
                    .append("call int64 [mscorlib]System.Int64::Parse(string)")
                    .append("\n")
                    .append("stloc")
                    .append(" ")
                    .append(id);
        }
        if (id.startsWith("f_")) {
            code.append("call string [mscorlib]System.Console::ReadLine()")
                    .append("\n")
                    .append("call float64 [mscorlib]System.Double::Parse(string)")
                    .append("\n")
                    .append("stloc")
                    .append(" ")
                    .append(id);
        }
        if (id.startsWith("s_")) {
            code.append("call string [mscorlib]System.Console::ReadLine()")
                    .append("\n")
                    .append("stloc")
                    .append(" ")
                    .append(id);
        }
        if (id.startsWith("b_")) {
            code.append("call string [mscorlib]System.Console::ReadLine()")
                    .append("\n")
                    .append("call bool [mscorlib]System.Boolean::Parse(string)")
                    .append("\n")
                    .append("stloc")
                    .append(" ")
                    .append(id);
        }
        this.code.append("\n");
    }

    private void writeConstant(Token token) {
        code.append("ldstr " + token.getLexeme())
                .append("\n")
                .append("call void [mscorlib]System.Console::Write(string)")
                .append("\n");
    }

    private void write() {
        final String type = typeStack.pop();

        if (type.equals(INT_TYPE)) {
            code.append("conv.i8")
                    .append("\n");
        }

        final String printCommand = String.format("call void [mscorlib]System.Console::Write(%s)", type);
        code.append(printCommand)
                .append("\n");
    }

    // TODO: 24/11/2024 testar essa acao comando selecao - if
    private void selection() {
        final String rotulo1 = getRotuleName();
        pilhaRotulos.push(rotulo1);
        pilhaRotulos.push(getRotuleName());

        code.append("brfalse")
                .append(" ")
                .append(rotulo1)
                .append("\n");
    }

    private void selectCommandRotulos() {
        String rotuloDesempilhado2 = pilhaRotulos.pop();
        String rotuloDesempilhado1 = pilhaRotulos.pop();

        code.append("br")
                .append(" ")
                .append(rotuloDesempilhado1)
                .append("\n");
        pilhaRotulos.push(rotuloDesempilhado1);

        code.append(rotuloDesempilhado2)
                .append(":")
                .append("\n");
    }

    // TODO: 24/11/2024 testar e validar acao
    private void createRotulo(Token token) {
        String novoRotulo = getRotuleName();

        code.append("brfalse")
                .append(" ")
                .append(novoRotulo)
                .append("\n");

        pilhaRotulos.push(novoRotulo);
    }

    private void deleteRotulo() {
        final String rotulo = pilhaRotulos.pop();

        code.append(rotulo)
                .append(":")
                .append("\n");
    }

    // TODO: 24/11/2024 validar acao
    private void afterExpressao(Token token) {
        String rotuloDesempilhado = pilhaRotulos.pop();

        code.append("brtrue")
                .append(" ")
                .append(rotuloDesempilhado)
                .append("\n");
    }

    private void breakLine() {
        code.append("ldstr \"\\n\"")
                .append("\n")
                .append("call void [mscorlib]System.Console::Write(string) ")
                .append("\n");
    }

    private void generateHeader() {
        code = new StringBuilder(".assembly extern mscorlib {}\n" +
                ".assembly _codigo_objeto{}\n" +
                ".module _codigo_objeto.exe\n" +
                "\n" +
                ".class public _UNICA{\n" +
                "  .method static public void _principal(){\n" +
                "     .entrypoint \n");
    }

    private void generateFooter() {
        code.append("     ret\n" +
                "  }\n" +
                "}");
    }


    private void stackInt(Token token) {
        typeStack.push(INT_TYPE);
        code.append("ldc.i8")
                .append(" ")
                .append(token.getLexeme())
                .append("\n")
                .append("conv.r8")
                .append("\n");

    }

    private void stackFloat(Token token) {
        typeStack.push(FLOAT_TYPE);
        code.append("ldc.r8")
                .append(" ")
                .append(token.getLexeme().replace(",", "."))
                .append("\n");
    }

    private void stackString(Token token) {
        typeStack.push(STRING_TYPE);
        code.append("ldstr")
                .append(" ")
//                .append("\"")
                .append(token.getLexeme())
//                .append("\"")
                .append("\n");
    }

    private void and() {
        this.typeStack.pop();
        this.typeStack.pop();

        typeStack.push(BOOLEAN_TYPE);

        code.append("and")
                .append("\n");
    }

    private void or() {
        this.typeStack.pop();
        this.typeStack.pop();

        typeStack.push(BOOLEAN_TYPE);

        code.append("or")
                .append("\n");
    }

    private void stackTrue() {
        typeStack.push(BOOLEAN_TYPE);
        code.append("ldc.i4.1")
                .append("\n");
    }

    private void stackFalse() {
        typeStack.push(BOOLEAN_TYPE);
        code.append("ldc.i4.0")
                .append("\n");
    }
    private void subtraction() {
        typeStack.pop();
        typeStack.pop();
        code.append("sub")
                .append("\n");
    }

    private void saveRelationalOperation(Token token) {
        operadorRelacional = token.getLexeme();
    }

    private void performRelationalOperation(Token token) {
        switch (this.operadorRelacional) {
            case "==":
                code.append("ceq");
                break;
            case "!=":
                code.append("ceq")
                        .append("\n")
                        .append("ldc.i4.1") // TODO: 23/11/2024 validar implementaçao do operador "diferente"
                        .append("\n")
                        .append("xor");
                break;
            case ">":
                code.append("cgt");
                break;
            case "<":
                code.append("clt");
                break;
            default:
                System.out.println("Default -> relational operation not identified...");
        }
        code.append("\n");
        typeStack.push(BOOLEAN_TYPE);
    }


    private void add() {
        String firstOperandType = this.typeStack.pop();
        String secondOperandType = this.typeStack.pop();

        if (firstOperandType.equals(FLOAT_TYPE) || secondOperandType.equals(FLOAT_TYPE)) {
            typeStack.push(FLOAT_TYPE);
        } else typeStack.push(INT_TYPE);

        code.append("add")
                .append("\n");
    }

    private void sub() {
        String firstOperandType = this.typeStack.pop();
        String secondOperandType = this.typeStack.pop();

        if (firstOperandType.equals(FLOAT_TYPE) || secondOperandType.equals(FLOAT_TYPE)) {
            typeStack.push(FLOAT_TYPE);
        } else typeStack.push(INT_TYPE);

        code.append("sub")
                .append("\n");
    }

    private void mul() {
        String firstOperandType = this.typeStack.pop();
        String secondOperandType = this.typeStack.pop();

        if (firstOperandType.equals(FLOAT_TYPE) || secondOperandType.equals(FLOAT_TYPE)) {
            typeStack.push(FLOAT_TYPE);
        } else typeStack.push(INT_TYPE);

        code.append("mul")
                .append("\n");
    }

    private void div() {
        this.typeStack.pop();
        this.typeStack.pop();

        typeStack.push(FLOAT_TYPE);
        code.append("div")
                .append("\n");
    }

    // TODO: 23/11/2024 adicionar boolean na pilha de tipos
    private void not() {
        code.append("ldc.i4.1")
                .append("\n")
                .append("xor")
                .append("\n");
    }

    private void identifier(Token token) throws SemanticError {
        final String id = token.getLexeme();

        tabelaSimbolos.stream()
                .filter(v -> v.equals(id))
                .findFirst()
                .orElseThrow(() -> new SemanticError(id + " não declarado"));

        code.append("ldloc ")
                .append(id)
                .append("\n");

        if (id.startsWith("i_")) {
            typeStack.push(INT_TYPE);
            code.append("conv.r8")
                    .append("\n");
        } else if (id.startsWith("f_")) {
            typeStack.push(FLOAT_TYPE);
        } else if (id.startsWith("s_")) {
            typeStack.push(STRING_TYPE);
        } else {
            typeStack.push(BOOLEAN_TYPE);
        }

    }

    private String getRotuleName() {
        rotuleCounter++;
        return "L"+rotuleCounter;
    }

    // TODO - Remove after tests
    public void printCode() {
        System.out.println("");
        System.out.println("-------------------");
        System.out.println(code.toString());
    }

    public String getCode() {
        return code.toString();
    }

}
