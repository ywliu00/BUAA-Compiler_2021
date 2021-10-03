import Exceptions.SyntaxException;
import SyntaxClasses.SyntaxClass;
import SyntaxClasses.Token;

import java.util.ArrayList;
import java.util.LinkedList;

public class SyntaxAnalyzer {
    private ArrayList<Token> tokenList;
    private SyntaxClass globalCompUnit;
    private int pos;

    public SyntaxAnalyzer() {
        this.globalCompUnit = null;
        this.pos = 0;
    }

    public void setTokenList(LinkedList<Token> tokenList) {
        this.tokenList = new ArrayList<>();
        this.tokenList.addAll(tokenList);
    }

    public int getPos() {
        return pos;
    }

    public SyntaxClass getGlobalCompUnit() {
        return globalCompUnit;
    }

    public void syntaxAnalyze() throws SyntaxException {
        globalCompUnit = readCompUnit();
    }

    public SyntaxClass readCompUnit() throws SyntaxException {
        SyntaxClass compUnit = new SyntaxClass(SyntaxClass.COMPUNIT);
        if (pos >= tokenList.size()) {
            return null;
        }
        // 检查是否有Decl成分，如果没有，弹出错误，就break
        SyntaxClass decl = null;
        int startPos = pos;
        while (true) {
            try {
                startPos = pos;
                decl = readDecl();
            } catch (SyntaxException e) {
                pos = startPos;
                break;
            }
            if (decl == null) break;
            compUnit.appendSonNode(decl);
        }
        // 检查是否有FuncDef成分，如果没有，弹出错误，就break
        SyntaxClass funcDef = null;
        while (true) {
            try {
                startPos = pos;
                funcDef = readFuncDef();
            } catch (SyntaxException e) {
                pos = startPos;
                break;
            }
            if (funcDef == null) break;
            compUnit.appendSonNode(funcDef);
        }
        // 分析MainFuncDef成分
        SyntaxClass mainFuncDef;
        startPos = pos;
        mainFuncDef = readMainFuncDef();
        if (mainFuncDef == null) {
            pos = startPos;
            throw new SyntaxException();
        }
        compUnit.appendSonNode(mainFuncDef);
        compUnit.setFirstAsLineNo(); // 设置非终结符行号
        return compUnit;
    }

    public SyntaxClass readDecl() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        int startPos;
        SyntaxClass decl = new SyntaxClass(SyntaxClass.DECL);
        // 开头是const，说明是ConstDecl
        if (tokenList.get(pos).getTokenType() == Token.CONSTTK) {
            SyntaxClass constDecl;
            startPos = pos;
            constDecl = readConstDecl();
            if (constDecl == null) {
                pos = startPos;
                throw new SyntaxException();
            } else {
                decl.appendSonNode(constDecl);
            }
        } else { // 否则是VarDecl
            SyntaxClass varDecl;
            startPos = pos;
            varDecl = readVarDecl();
            if (varDecl == null) {
                pos = startPos;
                throw new SyntaxException();
            } else {
                decl.appendSonNode(varDecl);
            }
        }
        decl.setFirstAsLineNo();
        return decl;
    }

    public SyntaxClass readConstDecl() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        int startPos;
        // 检查是否以const开头
        if (tokenList.get(pos).getTokenType() != Token.CONSTTK) {
            return null;
        }
        SyntaxClass constDecl = new SyntaxClass(SyntaxClass.CONSTDECL),
                bType, constDef;
        Token constToken = tokenList.get(this.pos++);
        constDecl.appendSonNode(constToken);
        // 检查BType
        startPos = pos;
        bType = readBType();
        if (bType == null) {
            pos = startPos;
            throw new SyntaxException();
        } else {
            constDecl.appendSonNode(bType);
        }
        // 检查必需的constDef
        startPos = pos;
        constDef = readConstDef();
        if (constDef == null) {
            pos = startPos;
            throw new SyntaxException();
        } else {
            constDecl.appendSonNode(constDef);
        }

        // 如果有逗号就继续
        while (tokenList.get(pos).getTokenType() == Token.COMMA) {
            Token comma = tokenList.get(pos++);
            startPos = pos;
            constDef = readConstDef();
            if (constDef == null) {
                pos = startPos;
                throw new SyntaxException();
            } else {
                constDecl.appendSonNode(comma);
                constDecl.appendSonNode(constDef);
            }
        }

        // 检测分号
        if (tokenList.get(pos).getTokenType() != Token.SEMICN) {
            throw new SyntaxException();
        } else {
            Token semicn = tokenList.get(pos++);
            constDecl.appendSonNode(semicn);
        }
        constDecl.setFirstAsLineNo();
        return constDecl;
    }

    public SyntaxClass readBType() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass bType = new SyntaxClass(SyntaxClass.BTYPE);
        // 检查是不是int
        if (tokenList.get(pos).getTokenType() != Token.INTTK) {
            throw new SyntaxException();
        } else {
            bType.appendSonNode(tokenList.get(pos++));
        }
        return bType;
    }

    public SyntaxClass readConstDef() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass constDef = new SyntaxClass(SyntaxClass.CONSTDEF);
        Token ident = tokenList.get(pos);
        int startPos;
        // 检查是否是标识符
        if (ident.getTokenType() != Token.IDENFR) {
            throw new SyntaxException();
        } else {
            ++pos;
            constDef.appendSonNode(ident);
        }
        // 检查是否有左中括号
        Token token;
        while (tokenList.get(pos).getTokenType() == Token.LBRACK) {
            // 若有，检查ConstExp和右中括号
            token = tokenList.get(pos++);
            SyntaxClass constExp;
            startPos = pos;
            constExp = readConstExp();
            Token rbrack = tokenList.get(pos);
            if (rbrack.getTokenType() != Token.RBRACK) {
                throw new SyntaxException();
            } else {
                ++pos;
                constDef.appendSonNode(token);
                constDef.appendSonNode(constExp);
                constDef.appendSonNode(rbrack);
            }
        }
        // 检查是否是 =
        if (tokenList.get(pos).getTokenType() != Token.ASSIGN) {
            throw new SyntaxException();
        } else {
            token = tokenList.get(pos++);
            constDef.appendSonNode(token);
            SyntaxClass constInitVal;
            // 检查constInitVal
            constInitVal = readConstInitVal();
            if (constInitVal == null) {
                throw new SyntaxException();
            } else {
                constDef.appendSonNode(constInitVal);
            }
        }
        constDef.setFirstAsLineNo();
        return constDef;
    }

    public SyntaxClass readConstInitVal() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass constInitVal = new SyntaxClass(SyntaxClass.CONSTINITVAL);
        // 检查是否是左花括号
        if (tokenList.get(pos).getTokenType() == Token.LBRACE) {
            Token token = tokenList.get(pos++);
            constInitVal.appendSonNode(token);
            // 先看看是不是右花括号，如果不是，说明中间有内容
            // 这样的话可以避免回溯
            if (tokenList.get(pos).getTokenType() != Token.RBRACE) {
                // 检查是否是ConstInitVal
                SyntaxClass subConstInitVal;
                subConstInitVal = readConstInitVal();
                if (subConstInitVal == null) {
                    throw new SyntaxException();
                } else {
                    constInitVal.appendSonNode(subConstInitVal);
                }
                // 检查有无逗号
                while (tokenList.get(pos).getTokenType() == Token.COMMA) {
                    Token comma = tokenList.get(pos++);
                    constInitVal.appendSonNode(comma);
                    // 有逗号，后面需要再接ConstInitVal
                    subConstInitVal = readConstInitVal();
                    if (subConstInitVal == null) {
                        throw new SyntaxException();
                    } else {
                        constInitVal.appendSonNode(subConstInitVal);
                    }
                }
            }
            // 检查右花括号
            if (tokenList.get(pos).getTokenType() != Token.RBRACE) {
                throw new SyntaxException();
            } else {
                token = tokenList.get(pos++);
                constInitVal.appendSonNode(token);
            }
        } else { // 不是左花括号，要匹配一个ConstExp
            SyntaxClass constExp;
            constExp = readConstExp();
            if (constExp == null) {
                throw new SyntaxException();
            } else {
                constInitVal.appendSonNode(constExp);
            }
        }
        constInitVal.setFirstAsLineNo();
        return constInitVal;
    }

    public SyntaxClass readVarDecl() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass varDecl = new SyntaxClass(SyntaxClass.VARDECL);
        SyntaxClass bType, varDef;
        // 检查BType
        bType = readBType();
        if (bType == null) {
            throw new SyntaxException();
        } else {
            varDecl.appendSonNode(bType);
        }
        // 检查VarDef
        varDef = readVarDef();
        if (varDef == null) {
            throw new SyntaxException();
        } else {
            varDecl.appendSonNode(varDef);
        }
        Token token;
        // 如果有逗号
        while (tokenList.get(pos).getTokenType() == Token.COMMA) {
            token = tokenList.get(pos++);
            varDecl.appendSonNode(token);
            // 逗号后面需要是VarDef
            varDef = readVarDef();
            if (varDef == null) {
                throw new SyntaxException();
            } else {
                varDecl.appendSonNode(varDef);
            }
        }
        // 需要一个分号
        if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
            token = tokenList.get(pos++);
            varDecl.appendSonNode(token);
        } else {
            throw new SyntaxException();
        }
        varDecl.setFirstAsLineNo();
        return varDecl;
    }

    public SyntaxClass readVarDef() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass varDef = new SyntaxClass(SyntaxClass.VARDEF);
        Token ident = tokenList.get(pos);
        // 检查Token是否是标识符
        if (ident.getTokenType() != Token.IDENFR) {
            throw new SyntaxException();
        } else {
            ++pos;
            varDef.appendSonNode(ident);
        }
        // 检查是否有左中括号
        Token token = tokenList.get(pos);
        while (token.getTokenType() == Token.LBRACK) {
            // 若有，检查ConstExp和右中括号
            ++pos;
            SyntaxClass constExp;
            constExp = readConstExp();
            Token rbrack = tokenList.get(pos);
            if (rbrack.getTokenType() != Token.RBRACK) {
                throw new SyntaxException();
            } else {
                ++pos;
                varDef.appendSonNode(token);
                varDef.appendSonNode(constExp);
                varDef.appendSonNode(rbrack);
            }
            token = tokenList.get(pos);
        }
        // 如果是 =
        if (token.getTokenType() == Token.ASSIGN) {
            ++pos;
            varDef.appendSonNode(token);
            SyntaxClass initVal;
            // 检查InitVal
            initVal = readInitVal();
            if (initVal == null) {
                throw new SyntaxException();
            } else {
                varDef.appendSonNode(initVal);
            }
        }
        varDef.setFirstAsLineNo();
        return varDef;
    }

    public SyntaxClass readInitVal() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass initVal = new SyntaxClass(SyntaxClass.INITVAL);
        Token token;
        // 检查是否是左花括号
        if (tokenList.get(pos).getTokenType() == Token.LBRACE) {
            token = tokenList.get(pos++);
            initVal.appendSonNode(token);
            // 如果不是右花括号，说明中间有东西，避免回溯
            if (tokenList.get(pos).getTokenType() != Token.RBRACE) {
                // 检查必须存在的InitVal
                SyntaxClass subInitVal;
                subInitVal = readInitVal();
                if (subInitVal == null) {
                    throw new SyntaxException();
                } else {
                    initVal.appendSonNode(subInitVal);
                }
                // 有逗号，可以继续读
                while (tokenList.get(pos).getTokenType() == Token.COMMA) {
                    token = tokenList.get(pos++);
                    initVal.appendSonNode(token);
                    // 需要一个InitVal
                    subInitVal = readInitVal();
                    if (subInitVal == null) {
                        throw new SyntaxException();
                    } else {
                        initVal.appendSonNode(subInitVal);
                    }
                }
            }
            // 检查右花括号
            if (tokenList.get(pos).getTokenType() == Token.RBRACE) {
                token = tokenList.get(pos++);
                initVal.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
        } else {
            // 单Exp情况
            SyntaxClass exp;
            exp = readExp();
            if (exp == null) {
                throw new SyntaxException();
            } else {
                initVal.appendSonNode(exp);
            }
        }
        initVal.setFirstAsLineNo();
        return initVal;
    }

    public SyntaxClass readFuncDef() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass funcDef = new SyntaxClass(SyntaxClass.FUNCDEF);
        SyntaxClass funcType, block;
        // 检查FuncType
        funcType = readFuncType();
        if (funcType == null) {
            throw new SyntaxException();
        } else {
            funcDef.appendSonNode(funcType);
        }
        // 检查Ident
        if (tokenList.get(pos).getTokenType() != Token.IDENFR) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            funcDef.appendSonNode(ident);
        }
        // 检查左括号
        if (tokenList.get(pos).getTokenType() != Token.LPARENT) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            funcDef.appendSonNode(ident);
        }
        // 若没有直接遇见右括号，说明中间有东西
        if (tokenList.get(pos).getTokenType() != Token.RPARENT) {
            SyntaxClass funcFParams;
            funcFParams = readFuncFParams();
            if (funcFParams == null) {
                throw new SyntaxException();
            } else {
                funcDef.appendSonNode(funcFParams);
            }
        }
        // 检查右括号
        if (tokenList.get(pos).getTokenType() != Token.RPARENT) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            funcDef.appendSonNode(ident);
        }
        // 检查Block
        block = readBlock();
        if (block == null) {
            throw new SyntaxException();
        } else {
            funcDef.appendSonNode(block);
        }
        funcDef.setFirstAsLineNo();
        return funcDef;
    }

    public SyntaxClass readMainFuncDef() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass mainFuncDef = new SyntaxClass(SyntaxClass.MAINFUNCDEF);
        SyntaxClass block;
        // 检查int
        if (tokenList.get(pos).getTokenType() != Token.INTTK) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            mainFuncDef.appendSonNode(ident);
        }
        // 检查main
        if (tokenList.get(pos).getTokenType() != Token.MAINTK) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            mainFuncDef.appendSonNode(ident);
        }
        // 检查左括号
        if (tokenList.get(pos).getTokenType() != Token.LPARENT) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            mainFuncDef.appendSonNode(ident);
        }
        // 检查右括号
        if (tokenList.get(pos).getTokenType() != Token.RPARENT) {
            throw new SyntaxException();
        } else {
            Token ident;
            ident = tokenList.get(pos++);
            mainFuncDef.appendSonNode(ident);
        }
        // 检查Block
        block = readBlock();
        if (block == null) {
            throw new SyntaxException();
        } else {
            mainFuncDef.appendSonNode(block);
        }
        mainFuncDef.setFirstAsLineNo();
        return mainFuncDef;
    }

    public SyntaxClass readFuncType() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass funcType = new SyntaxClass(SyntaxClass.FUNCTYPE);
        // 检查void
        if (tokenList.get(pos).getTokenType() == Token.VOIDTK ||
                tokenList.get(pos).getTokenType() == Token.INTTK) {
            Token ident;
            ident = tokenList.get(pos++);
            funcType.appendSonNode(ident);
        } else {
            throw new SyntaxException();
        }
        funcType.setFirstAsLineNo();
        return funcType;
    }

    public SyntaxClass readFuncFParams() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass funcFParams = new SyntaxClass(SyntaxClass.FUNCFPARAMS);
        SyntaxClass param;
        // 检查必须有的FuncFParam
        param = readFuncFParam();
        if (param == null) {
            throw new SyntaxException();
        } else {
            funcFParams.appendSonNode(param);
        }
        // 如果有逗号，说明后面还有
        while (tokenList.get(pos).getTokenType() == Token.COMMA) {
            Token token = tokenList.get(pos++);
            funcFParams.appendSonNode(token);
            param = readFuncFParam();
            if (param == null) {
                throw new SyntaxException();
            } else {
                funcFParams.appendSonNode(param);
            }
        }
        funcFParams.setFirstAsLineNo();
        return funcFParams;
    }

    public SyntaxClass readFuncFParam() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass funcFParam = new SyntaxClass(SyntaxClass.FUNCFPARAM);
        // 检查BType
        SyntaxClass bType;
        bType = readBType();
        if (bType == null) {
            throw new SyntaxException();
        } else {
            funcFParam.appendSonNode(bType);
        }
        // 检查ident
        if (tokenList.get(pos).getTokenType() == Token.IDENFR) {
            Token ident = tokenList.get(pos++);
            funcFParam.appendSonNode(ident);
        } else {
            throw new SyntaxException();
        }
        // 如果有左中括号
        if (tokenList.get(pos).getTokenType() == Token.LBRACK) {
            Token ident = tokenList.get(pos++);
            funcFParam.appendSonNode(ident);
            // 需要跟一个右中括号
            if (tokenList.get(pos).getTokenType() == Token.RBRACK) {
                ident = tokenList.get(pos++);
                funcFParam.appendSonNode(ident);
            } else {
                throw new SyntaxException();
            }
            // 如果还有左中括号
            while (tokenList.get(pos).getTokenType() == Token.LBRACK) {
                ident = tokenList.get(pos++);
                funcFParam.appendSonNode(ident);
                // 检查一个ConstExp
                SyntaxClass constExp;
                constExp = readConstExp();
                if (constExp == null) {
                    throw new SyntaxException();
                }
                funcFParam.appendSonNode(constExp);
                // 检查右中括号
                if (tokenList.get(pos).getTokenType() == Token.RBRACK) {
                    ident = tokenList.get(pos++);
                    funcFParam.appendSonNode(ident);
                } else {
                    throw new SyntaxException();
                }
            }
        }
        funcFParam.setFirstAsLineNo();
        return funcFParam;
    }

    public SyntaxClass readBlock() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass block = new SyntaxClass(SyntaxClass.BLOCK);
        // 检查左花括号
        int startPos = pos;
        Token brace;
        if (tokenList.get(pos).getTokenType() == Token.LBRACE) {
            brace = tokenList.get(pos++);
            block.appendSonNode(brace);
            SyntaxClass blockItem;
            // 没见到右花括号就继续
            while (tokenList.get(pos).getTokenType() != Token.RBRACE) {
                if (pos >= tokenList.size()) {
                    throw new SyntaxException();
                }
                // 可能存在的BlockItem，若发生错误，则说明没有，break
                // 准备回溯（疑似无必要）
                startPos = pos;
                try {
                    blockItem = readBlockItem();
                } catch (SyntaxException e) {
                    pos = startPos;
                    break;
                }
                if (blockItem == null) {
                    throw new SyntaxException();
                } else {
                    block.appendSonNode(blockItem);
                }
            }
            // 右花括号
            if (tokenList.get(pos).getTokenType() == Token.RBRACE) {
                brace = tokenList.get(pos++);
                block.appendSonNode(brace);
            } else {
                throw new SyntaxException();
            }
        } else {
            throw new SyntaxException();
        }
        block.setFirstAsLineNo();
        return block;
    }

    public SyntaxClass readBlockItem() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass blockItem = new SyntaxClass(SyntaxClass.BLOCKITEM);
        SyntaxClass syntaxClass;

        int startPos = pos, nextTokenType = tokenList.get(pos).getTokenType();
        if (nextTokenType == Token.CONSTTK || nextTokenType == Token.INTTK) {
            // 尝试解析Decl
            syntaxClass = readDecl();
            if (syntaxClass == null) {
                throw new SyntaxException();
            }
            blockItem.appendSonNode(syntaxClass);
        } else {
            // 尝试解析Stmt
            // 先回溯
            // pos = startPos;
            syntaxClass = readStmt();
            if (syntaxClass != null) {
                blockItem.appendSonNode(syntaxClass);
            } else {
                throw new SyntaxException();
            }
        }
        blockItem.setFirstAsLineNo();
        return blockItem;
    }

    public SyntaxClass readStmt() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass stmt = new SyntaxClass(SyntaxClass.STMT);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // If
        if (nextTokenType == Token.IFTK) {
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 左括号
            if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // Cond
            SyntaxClass cond = readCond();
            if (cond == null) {
                throw new SyntaxException();
            } else {
                stmt.appendSonNode(cond);
            }
            // 右括号
            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // Stmt
            SyntaxClass subStmt = readStmt();
            if (subStmt == null) {
                throw new SyntaxException();
            } else {
                stmt.appendSonNode(subStmt);
            }
            // 如果有else
            if (tokenList.get(pos).getTokenType() == Token.ELSETK) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
                // Stmt
                subStmt = readStmt();
                if (subStmt == null) {
                    throw new SyntaxException();
                } else {
                    stmt.appendSonNode(subStmt);
                }
            }
        } else if (nextTokenType == Token.WHILETK) {
            // While
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 左括号
            if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // Cond
            SyntaxClass cond = readCond();
            if (cond == null) {
                throw new SyntaxException();
            } else {
                stmt.appendSonNode(cond);
            }
            // 右括号
            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // Stmt
            SyntaxClass subStmt = readStmt();
            if (subStmt == null) {
                throw new SyntaxException();
            } else {
                stmt.appendSonNode(subStmt);
            }
        } else if (nextTokenType == Token.BREAKTK) {
            // Break
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 分号
            if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
        } else if (nextTokenType == Token.CONTINUETK) {
            // Continue
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 分号
            if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
        } else if (nextTokenType == Token.RETURNTK) {
            // Return
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 分号
            if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                // 没有分号，说明有返回值
                SyntaxClass exp;
                exp = readExp();
                if (exp == null) {
                    throw new SyntaxException();
                } else {
                    stmt.appendSonNode(exp);
                }
                // 分号
                if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                    token = tokenList.get(pos++);
                    stmt.appendSonNode(token);
                } else {
                    throw new SyntaxException();
                }
            }
        } else if (nextTokenType == Token.PRINTFTK) {
            // Printf
            Token token = tokenList.get(pos++);
            stmt.appendSonNode(token);
            // 左括号
            if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // FormatString
            if (tokenList.get(pos).getTokenType() == Token.STRCON) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // 如果有逗号
            while (tokenList.get(pos).getTokenType() == Token.COMMA) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
                // 需要有Exp
                SyntaxClass exp = readExp();
                if (exp == null) {
                    throw new SyntaxException();
                } else {
                    stmt.appendSonNode(exp);
                }
            }
            // 右括号
            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
            // 分号
            if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                token = tokenList.get(pos++);
                stmt.appendSonNode(token);
            } else {
                throw new SyntaxException();
            }
        } else if (nextTokenType == Token.LBRACE) {
            // 左花括号，说明是一个Block
            SyntaxClass block = readBlock();
            if (block == null) {
                throw new SyntaxException();
            } else {
                stmt.appendSonNode(block);
            }
        } else {
            /* LVal=Exp,[Exp],LVal=getint()三种情况，存在回溯可能*/
            int startPos = pos;
            // [Exp];中单走一个分号的情况（空语句）
            Token semicn;
            if (nextTokenType == Token.SEMICN) {
                semicn = tokenList.get(pos++);
                stmt.appendSonNode(semicn);
            } else { // 非空语句
                // 先试试LVal
                SyntaxClass lVal = null;
                boolean isLVal = true;
                try {
                    lVal = readLVal();
                } catch (SyntaxException e) {
                    isLVal = false;
                }
                if (lVal == null) {
                    isLVal = false;
                }
                if (isLVal) {
                    // 检查是否跟着等号
                    if (tokenList.get(pos).getTokenType() == Token.ASSIGN) {
                        Token token = tokenList.get(pos++);
                        // 确实是等号，则LVal可以确认加入
                        stmt.appendSonNode(lVal);
                        stmt.appendSonNode(token);
                        // 检查是否是getint
                        if (tokenList.get(pos).getTokenType() == Token.GETINTTK) {
                            token = tokenList.get(pos++);
                            // 是getint
                            stmt.appendSonNode(token);
                            // 左括号
                            if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
                                token = tokenList.get(pos++);
                                stmt.appendSonNode(token);
                            } else {
                                throw new SyntaxException(); // getint 左括号缺失
                            }
                            // 右括号
                            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                                token = tokenList.get(pos++);
                                stmt.appendSonNode(token);
                            } else {
                                throw new SyntaxException(); // getint 右括号缺失
                            }
                        } else {
                            // 不是getint，则应该是Exp
                            SyntaxClass exp;
                            exp = readExp();
                            if (exp == null) {
                                throw new SyntaxException();
                            }
                            stmt.appendSonNode(exp);
                        }
                    } else {
                        // LVal后面没跟等号，说明不可以解析为LVal，应该是Exp的一部分
                        isLVal = false;
                    }
                }
                if (!isLVal) {
                    // 经过前面所有判断，认为不是单独的LVal，只能是非空的Exp，需要回溯
                    pos = startPos;
                    SyntaxClass exp = readExp();
                    if (exp == null) {
                        throw new SyntaxException();
                    }
                    stmt.appendSonNode(exp);
                }
                // 分号
                if (tokenList.get(pos).getTokenType() == Token.SEMICN) {
                    semicn = tokenList.get(pos++);
                    stmt.appendSonNode(semicn);
                } else {
                    throw new SyntaxException();
                }
            }
        }
        stmt.setFirstAsLineNo();
        return stmt;
    }

    public SyntaxClass readExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass exp = new SyntaxClass(SyntaxClass.EXP), addExp;
        // 检查AddExp
        addExp = readAddExp();
        if (addExp == null) {
            throw new SyntaxException();
        }
        exp.appendSonNode(addExp);
        return exp;
    }

    public SyntaxClass readCond() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass exp = new SyntaxClass(SyntaxClass.COND), lOrExp;
        // 检查LOrExp
        lOrExp = readLOrExp();
        if (lOrExp == null) {
            throw new SyntaxException();
        }
        exp.appendSonNode(lOrExp);
        return exp;
    }

    public SyntaxClass readLVal() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass lVal = new SyntaxClass(SyntaxClass.LVAL);
        // 检查Ident
        Token ident;
        if (tokenList.get(pos).getTokenType() == Token.IDENFR) {
            ident = tokenList.get(pos++);
            lVal.appendSonNode(ident);
            // 检查可能有的左中括号
            while (tokenList.get(pos).getTokenType() == Token.LBRACK) {
                Token brack = tokenList.get(pos++);
                lVal.appendSonNode(brack);
                SyntaxClass exp;
                // Exp
                exp = readExp();
                if (exp == null) {
                    throw new SyntaxException();
                }
                lVal.appendSonNode(exp);
                // 右中括号
                if (tokenList.get(pos).getTokenType() == Token.RBRACK) {
                    brack = tokenList.get(pos++);
                    lVal.appendSonNode(brack);
                } else {
                    throw new SyntaxException();
                }
            }
        } else {
            throw new SyntaxException();
        }
        lVal.setFirstAsLineNo();
        return lVal;
    }

    public SyntaxClass readPrimaryExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass primaryExp = new SyntaxClass(SyntaxClass.PRIMARYEXP);
        // (Exp)
        if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
            // (
            Token parent = tokenList.get(pos++);
            primaryExp.appendSonNode(parent);
            // Exp
            SyntaxClass exp;
            exp = readExp();
            if (exp == null) {
                throw new SyntaxException();
            }
            primaryExp.appendSonNode(exp);
            // )
            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                parent = tokenList.get(pos++);
                primaryExp.appendSonNode(parent);
            } else {
                throw new SyntaxException();
            }
        } else if (tokenList.get(pos).getTokenType() == Token.INTCON) {
            // Number
            SyntaxClass number;
            number = readNumber();
            if (number == null) {
                throw new SyntaxException();
            }
            primaryExp.appendSonNode(number);
        } else {
            // LVal
            SyntaxClass lVal;
            lVal = readLVal();
            if (lVal == null) {
                throw new SyntaxException();
            }
            primaryExp.appendSonNode(lVal);
        }
        primaryExp.setFirstAsLineNo();
        return primaryExp;
    }

    public SyntaxClass readNumber() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass number = new SyntaxClass(SyntaxClass.NUMBER);
        if (tokenList.get(pos).getTokenType() == Token.INTCON) {
            // IntConst
            Token parent = tokenList.get(pos++);
            number.appendSonNode(parent);
        } else {
            throw new SyntaxException();
        }
        number.setFirstAsLineNo();
        return number;
    }

    public SyntaxClass readUnaryExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass unaryExp = new SyntaxClass(SyntaxClass.UNARYEXP);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // Ident
        if (nextTokenType == Token.IDENFR && tokenList.get(pos + 1).getTokenType() == Token.LPARENT) {
            Token ident = tokenList.get(pos++);
            unaryExp.appendSonNode(ident);
            // 左括号
            if (tokenList.get(pos).getTokenType() == Token.LPARENT) {
                Token lParent = tokenList.get(pos++);
                unaryExp.appendSonNode(lParent);
            } else {
                throw new SyntaxException();
            }
            // 可能的参数
            SyntaxClass funcRParams;
            int startPos = pos;
            try {
                funcRParams = readFuncRParams();
            } catch (SyntaxException e) {
                // 出错说明没有能够读取到参数，那就是没有
                funcRParams = null;
                pos = startPos;
            }
            if (funcRParams != null) {
                unaryExp.appendSonNode(funcRParams);
            }
            // 右括号
            if (tokenList.get(pos).getTokenType() == Token.RPARENT) {
                Token rParent = tokenList.get(pos++);
                unaryExp.appendSonNode(rParent);
            } else {
                throw new SyntaxException();
            }
        } else if (nextTokenType == Token.PLUS ||
                nextTokenType == Token.MINU ||
                nextTokenType == Token.NOT) {
            // UnaryOp UnaryExp情况
            // 先检查一元运算符
            SyntaxClass unaryOp;
            unaryOp = readUnaryOp();
            if (unaryOp == null) {
                throw new SyntaxException();
            }
            unaryExp.appendSonNode(unaryOp);
            // 检查一元表达式
            SyntaxClass subUnaryExp;
            subUnaryExp = readUnaryExp();
            if (subUnaryExp == null) {
                throw new SyntaxException();
            }
            unaryExp.appendSonNode(subUnaryExp);
        } else {
            // PrimaryExp
            SyntaxClass primaryExp;
            primaryExp = readPrimaryExp();
            if (primaryExp == null) {
                throw new SyntaxException();
            }
            unaryExp.appendSonNode(primaryExp);
        }
        unaryExp.setFirstAsLineNo();
        return unaryExp;
    }

    public SyntaxClass readUnaryOp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass unaryOp = new SyntaxClass(SyntaxClass.UNARYOP);
        // +,-,!
        int tokenType = tokenList.get(pos).getTokenType();
        if (tokenType == Token.PLUS || tokenType == Token.MINU || tokenType == Token.NOT) {
            Token unaryOpToken = tokenList.get(pos++);
            unaryOp.appendSonNode(unaryOpToken);
        } else {
            throw new SyntaxException();
        }
        unaryOp.setFirstAsLineNo();
        return unaryOp;
    }

    public SyntaxClass readFuncRParams() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass funcRParams = new SyntaxClass(SyntaxClass.FUNCRPARAMS);
        // 必须要有的Exp
        SyntaxClass exp;
        exp = readExp();
        if (exp == null) {
            throw new SyntaxException();
        }
        funcRParams.appendSonNode(exp);
        // 如果有逗号就一直读
        while (tokenList.get(pos).getTokenType() == Token.COMMA) {
            Token comma = tokenList.get(pos++);
            funcRParams.appendSonNode(comma);
            exp = readExp();
            if (exp == null) {
                throw new SyntaxException();
            }
            funcRParams.appendSonNode(exp);
        }
        funcRParams.setFirstAsLineNo();
        return funcRParams;
    }

    public SyntaxClass readMulExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass mulExp = new SyntaxClass(SyntaxClass.MULEXP);
        SyntaxClass unaryExp;
        // 修改文法，消除左递归
        // 需要一个UnaryExp
        unaryExp = readUnaryExp();
        if (unaryExp == null) {
            throw new SyntaxException();
        }
        mulExp.appendSonNode(unaryExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // *, /, %
        while (nextTokenType == Token.MULT || nextTokenType == Token.DIV || nextTokenType == Token.MOD) {
            Token token = tokenList.get(pos++);
            mulExp.appendSonNode(token);
            // UnaryExp
            unaryExp = readUnaryExp();
            if (unaryExp == null) {
                throw new SyntaxException();
            }
            mulExp.appendSonNode(unaryExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        mulExp.setFirstAsLineNo();
        return mulExp;
    }

    public SyntaxClass readAddExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass addExp = new SyntaxClass(SyntaxClass.ADDEXP);
        SyntaxClass mulExp;
        // 修改文法，消除左递归
        // 需要一个MulExp
        mulExp = readMulExp();
        if (mulExp == null) {
            throw new SyntaxException();
        }
        addExp.appendSonNode(mulExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // +,-
        while (nextTokenType == Token.PLUS || nextTokenType == Token.MINU) {
            Token token = tokenList.get(pos++);
            addExp.appendSonNode(token);
            // MulExp
            mulExp = readMulExp();
            if (mulExp == null) {
                throw new SyntaxException();
            }
            addExp.appendSonNode(mulExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        addExp.setFirstAsLineNo();
        return addExp;
    }

    public SyntaxClass readRelExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass relExp = new SyntaxClass(SyntaxClass.RELEXP);
        SyntaxClass addExp;
        // 修改文法，消除左递归
        // 需要一个AddExp
        addExp = readAddExp();
        if (addExp == null) {
            throw new SyntaxException();
        }
        relExp.appendSonNode(addExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // <,>,<=,>=
        while (nextTokenType == Token.LSS || nextTokenType == Token.GRE ||
                nextTokenType == Token.LEQ || nextTokenType == Token.GEQ) {
            Token token = tokenList.get(pos++);
            relExp.appendSonNode(token);
            // AddExp
            addExp = readAddExp();
            if (addExp == null) {
                throw new SyntaxException();
            }
            relExp.appendSonNode(addExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        relExp.setFirstAsLineNo();
        return relExp;
    }

    public SyntaxClass readEqExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass eqExp = new SyntaxClass(SyntaxClass.EQEXP);
        SyntaxClass relExp;
        // 修改文法，消除左递归
        // 需要一个RelExp
        relExp = readRelExp();
        if (relExp == null) {
            throw new SyntaxException();
        }
        eqExp.appendSonNode(relExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // ==,!=
        while (nextTokenType == Token.EQL || nextTokenType == Token.NEQ) {
            Token token = tokenList.get(pos++);
            eqExp.appendSonNode(token);
            // RelExp
            relExp = readRelExp();
            if (relExp == null) {
                throw new SyntaxException();
            }
            eqExp.appendSonNode(relExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        eqExp.setFirstAsLineNo();
        return eqExp;
    }

    public SyntaxClass readLAndExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass lAndExp = new SyntaxClass(SyntaxClass.LANDEXP);
        SyntaxClass eqExp;
        // 修改文法，消除左递归
        // 需要一个EqExp
        eqExp = readEqExp();
        if (eqExp == null) {
            throw new SyntaxException();
        }
        lAndExp.appendSonNode(eqExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // &&
        while (nextTokenType == Token.AND) {
            Token token = tokenList.get(pos++);
            lAndExp.appendSonNode(token);
            // EqExp
            eqExp = readEqExp();
            if (eqExp == null) {
                throw new SyntaxException();
            }
            lAndExp.appendSonNode(eqExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        lAndExp.setFirstAsLineNo();
        return lAndExp;
    }

    public SyntaxClass readLOrExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass lOrExp = new SyntaxClass(SyntaxClass.LOREXP);
        SyntaxClass lAndExp;
        // 修改文法，消除左递归
        // 需要一个LAndExp
        lAndExp = readLAndExp();
        if (lAndExp == null) {
            throw new SyntaxException();
        }
        lOrExp.appendSonNode(lAndExp);
        int nextTokenType = tokenList.get(pos).getTokenType();
        // ||
        while (nextTokenType == Token.OR) {
            Token token = tokenList.get(pos++);
            lOrExp.appendSonNode(token);
            // LAndExp
            lAndExp = readLAndExp();
            if (lAndExp == null) {
                throw new SyntaxException();
            }
            lOrExp.appendSonNode(lAndExp);
            nextTokenType = tokenList.get(pos).getTokenType();
        }
        lOrExp.setFirstAsLineNo();
        return lOrExp;
    }

    public SyntaxClass readConstExp() throws SyntaxException {
        if (pos >= tokenList.size()) {
            return null;
        }
        SyntaxClass constExp = new SyntaxClass(SyntaxClass.CONSTEXP);
        // AddExp
        SyntaxClass addExp;
        addExp = readAddExp();
        if (addExp == null) {
            throw new SyntaxException();
        }
        constExp.appendSonNode(addExp);
        constExp.setFirstAsLineNo();
        return constExp;
    }
}
