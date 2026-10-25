package org.djutils.eval;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.djunits.quantity.Dimensionless;
import org.djunits.quantity.SIQuantity;
import org.djunits.quantity.Time;
import org.djunits.quantity.def.Quantity;
import org.djunits.unit.UnitInterface;
import org.djunits.unit.Unitless;
import org.djunits.unit.si.SIUnit;
import org.djunits.util.Constants;
import org.djutils.exceptions.Throw;
import org.djutils.metadata.MetaData;
import org.djutils.metadata.ObjectDescriptor;

/**
 * Eval - evaluate mathematical expression. Derived from software developed between 2002-2016 by Peter Knoppers.
 * <p>
 * CONSIDER: implement string datatype.
 * <p>
 * The precedence of binary operators follows the list of the
 * <a href="https://www.cs.bilkent.edu.tr/~guvenir/courses/CS101/op_precedence.html">Java Operator Precedence Table</a>,
 * skipping bitwise and other operators that make no sense for this evaluator and adding the exponentiation (^) operator.
 * <p>
 * @author Peter Knoppers
 */
public class Eval
{
    /** The expression evaluation stack. */
    private List<Object> stack = new ArrayList<>();

    /** The expression. */
    private String expression;

    /** Position of next token in the expression. */
    private int position = 0;

    /** Access to the variable pool. */
    private RetrieveValue retrieveValue = null;

    /** Binding strength of ternary conditional. */
    private static final int BIND_CONDITIONAL_EXPRESSION = 1;

    /** Binding strength of boolean OR operation. */
    private static final int BIND_OR = 2;

    /** Binding strength of boolean AND operation. */
    private static final int BIND_AND = 3;

    /** Binding strength of == and != operators. */
    private static final int BIND_EQUAL = 4;

    /** Binding strength of <, <=, >, >= relational operators. */
    private static final int BIND_RELATIONAL = 5;

    /** Binding strength of addition and subtraction operators. */
    private static final int BIND_ADD = 6;

    /** Binding strength of multiplication and division operators. */
    private static final int BIND_MUL = 7;

    /** Binding strength of the power operation. */
    private static final int BIND_POW = 8;

    /** Binding strength of unary minus and logical negation. */
    private static final int BIND_UMINUS = 9;

    /** Object descriptor array for all zero argument functions. */
    private static final ObjectDescriptor[] noArguments = new ObjectDescriptor[] {};

    /** The built-in functions. */
    // @formatter:off
    private final Function[] builtinFunctions = new Function[] {
        new F0("AVOGADRO", Constants.AVOGADRO, new MetaData("Avogadro constant", "Avogadro constant in 1/mol", noArguments)),
        new F0("BOLTZMANN", Constants.BOLTZMANN, new MetaData("Boltzmann constant",
                "The exact value of the Boltzmann constant in Joule per Kelvin", noArguments)),
        new F0("CESIUM133_FREQUENCY", Constants.CESIUM133_FREQUENCY, new MetaData("Cesium 133 frequency", 
                "The exact value of the Cesium 133 ground state hyperfine structure transition frequency", noArguments)),
        new F0("CURRENTTIME", Time.ofSi(0.0, Time.Reference.UNIX).getClass(),
                new MetaData("The current time in seconds since 1970 UTC", 
                        "The current time in seconds since 1970 UTC to the nearest ms as reported by the operating system", noArguments), 
                (f) -> Time.ofSi(System.currentTimeMillis() / 1000d, Time.Reference.UNIX)),
        new F0("E", Constants.E, new MetaData("Euler\'s constant e", "Euler\'s constant e; the base of the natural logarithm")),
        new F0("ELECTRONCHARGE", Constants.ELECTRONCHARGE, new MetaData("Electrical charge of one electron", 
                "The exact electrical charge of one electron", noArguments)),
        new F0("ELECTRONMASS", Constants.ELECTRONMASS, new MetaData("Mass of an electron", 
                "Mass of an electron, the value of this physical constant has an uncertainty of 2.8e-40 kg", noArguments)),
        new F0("G", Constants.G, new MetaData("Gravitational constant", 
                "Gravitational constant, a.k.a. Newtonian constant of gravitation. This is the 2018 best known approximation, which has an "
                + "uncertainty 1.5e-15 m^3/kgs^2", noArguments)),
        new F0("LIGHTSPEED", Constants.LIGHTSPEED, new MetaData("Speed of light in vacuum", "The exact speed of light in vacuum", noArguments)),
        new F0("LUMINOUS_EFFICACY_540THZ", Constants.LUMINOUS_EFFICACY_540THZ, new MetaData(
                "The luminous efficacy Kcd of monochromatic radiation of frequency 540×10^12 Hz (540 THz)",
                "The exact luminous efficacy Kcd of monochromatic radiation of frequency 540×10^12 Hz (540 THz)", noArguments)),
        new F0("NEUTRONMASS", Constants.NEUTRONMASS, new MetaData("Mass of a neutron", 
                "Mass of a neutron. The value of this physical constant has an uncertainty of 9.5e-37 kg.", noArguments)),
        new F0("PI", Constants.PI, new MetaData("Ratio of a half circumference of a circle and its radius", 
                "Ratio of a half circumference of a circle and its radius", noArguments)),
        new F0("PHI", Constants.PHI, new MetaData("Golden ratio", "Golden ratio", noArguments)),
        new F0("PLANCK", Constants.PLANCK, new MetaData("Planck constant; ratio of a photon's energy and its frequency",
                "Planck constant; the exact ratio of a photon's energy and its frequency", noArguments)),
        new F0("PLANCKREDUCED", Constants.PLANCKREDUCED, new MetaData("Reduced Planck constant", 
                "Reduced Planck constant, a.k.a. angular Planck constant; Planck constant divided by 2 pi" ,noArguments)),
        new F0("PROTONCHARGE", Constants.PROTONCHARGE, new MetaData("ElectricalCharge of one proton", "ElectricalCharge of one proton", noArguments)),
        new F0("PROTONMASS", Constants.PROTONMASS, new MetaData("Mass of a proton", 
                "Mass of a proton. The value of this physical constant has an uncertainty of 5.1e-37", noArguments)),
        new F0("TAU", Constants.TAU, new MetaData("Ratio of circumference of circle and its radius", 
                "Ratio of circumference of circle and its radius", noArguments)),
        new F0("VACUUMIMPEDANCE", Constants.VACUUMIMPEDANCE, new MetaData("Impedance of vacuum", "Impedance of vacuum", noArguments)),
        new F0("VACUUMPERMEABILITY", Constants.VACUUMPERMEABILITY, new MetaData("Permeability of vacuum",
                "Permeability of vacuum. The uncertainty of this value is 1.9e-16N/A^2", noArguments)),
        new F0("VACUUMPERMITTIVITY", Constants.VACUUMPERMITTIVITY, new MetaData("Permittivity of vacuum.",
                "Permittivity of vacuum. The uncertainty of this value is 1.3e-21 F/m.", noArguments)),
        new F0("TRUE", Boolean.TRUE, new MetaData("The logical value TRUE", "The logical value TRUE", noArguments)),
        new F0("FALSE", Boolean.FALSE, new MetaData("The logical value FALSE", "The logical value FALSE", noArguments)),
        new F1("acos", Dimensionless.class, new MetaData("acos", "returns the angle of which the cosine equals the value of the argument",
                new ObjectDescriptor("angle", "angle", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.acos(checkDimensionless(i, a).si()))),
        new F1("asin", Dimensionless.class, new MetaData("asin", "returns the angle of which the sine equals the value of the argument",
                new ObjectDescriptor("angle", "angle", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.asin(checkDimensionless(i, a).si()))),
        new F1("atan", Dimensionless.class, new MetaData("atan", "returns the angle of which the tangent equals the value of the argument",
                new ObjectDescriptor("angle", "angle", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.atan(checkDimensionless(i, a).si()))),
        new F1("cbrt", Dimensionless.class, new MetaData("cbrt", "returns the cubic root of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.cbrt(checkDimensionless(i, a).si()))),
        new F1("cos", Dimensionless.class, new MetaData("cos", "returns the cosine of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.cos(checkDimensionless(i, a).si()))),
        new F1("cosh", Dimensionless.class, new MetaData("cosh", "returns the hyperbolic cosine of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.cosh(checkDimensionless(i, a).si()))),
        new F1("exp", Dimensionless.class, new MetaData("exp", "returns e to the power of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.exp(checkDimensionless(i, a).si()))),
        new F1("expm1", Dimensionless.class, new MetaData("expm1", "returns e to the power of the argument minus 1",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.expm1(checkDimensionless(i, a).si()))),
        new F1("log", Dimensionless.class, new MetaData("log", "returns natural logarithm (logarithm base e) of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.log(checkDimensionless(i, a).si()))),
        new F1("log10", Dimensionless.class, new MetaData("log10", "returns logarithm base 10 of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.log10(checkDimensionless(i, a).si()))),
        new F1("log1p", Dimensionless.class, new MetaData("log1p", "returns natural logarithm (logarithm base e) of the argument plus 1",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.log1p(checkDimensionless(i, a).si()))),
        new F1("signum", Dimensionless.class, new MetaData("signum", "returns sign of the argument (1 if positive, -1 if negative, 0 if zero)",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.signum(checkDimensionless(i, a).si()))),
        new F1("sin", Dimensionless.class, new MetaData("cos", "returns the sine of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.sin(checkDimensionless(i, a).si()))),
        new F1("sinh", Dimensionless.class, new MetaData("cosh", "returns the hyperbolic sine of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.sinh(checkDimensionless(i, a).si()))),
        new F1("sqrt", Dimensionless.class, new MetaData("cos", "returns the square root of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.sqrt(checkDimensionless(i, a).si()))),
        new F1("tan", Dimensionless.class, new MetaData("cos", "returns the tangent of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.tan(checkDimensionless(i, a).si()))),
        new F1("tanh", Dimensionless.class, new MetaData("cosh", "returns the hyperbolic tangent of the value of the argument",
                new ObjectDescriptor("value", "value", Dimensionless.class)), 
                (i, a) -> Dimensionless.ofSi(Math.tanh(checkDimensionless(i, a).si()))),
        new F2("pow", new MetaData("pow", "raises the first argument to the power of the second argument",                
                new ObjectDescriptor("base", "base", Dimensionless.class), 
                new ObjectDescriptor("exponent", "exponent", Dimensionless.class)),
                (i, b, p)-> performPower(b, p) ),
        new F2("atan2", new MetaData("atan2", 
                "atan2 function (needs two Quantity parameters that have the same SI dimensions)", 
                new ObjectDescriptor("y", "y", Quantity.class),
                new ObjectDescriptor("x", "x", Quantity.class)), (i, y, x) -> performAtan2(y, x)),
        };
    
    // @formatter:on
    /** Map of all the built-in functions. */
    private Map<String, Function> functionData;

    {
        this.functionData = new HashMap<>();
        for (Function function : this.builtinFunctions)
        {
            this.functionData.put(function.getId(), function);
        }
    }

    /** User defined functions. */
    private Map<String, Function> userDefinedFunctions = null;

    /** User supplied unit parser. */
    private QuantityParser userSuppliedUnitParser = null;

    /** Map from Quantity sub classes to Quantities */
    private Map<Class<?>, SIUnit> siUnitsMap = new HashMap<>();

    /**
     * Construct a new evaluator with no RetrieveValue object and no added/overridden function and no added/overridden units.
     */
    public Eval()
    {
        // Nothing to do here
    }

    /**
     * Set or replace the RetrieveValue object of this evaluator.
     * @param retrieveValue the new RetrieveValue object (may be null to only delete the currently active RetrieveValue object).
     * @return this (for easy method chaining)
     */
    public Eval setRetrieveValue(final RetrieveValue retrieveValue)
    {
        this.retrieveValue = retrieveValue;
        return this;
    }

    /**
     * Install a unit parser (or replace or remove a previously installed unit parser). A user supplied unit parser takes
     * precedence over the built-in unit parser (that can only handle SI strings; see SIUnit.of).
     * @param unitParser the new unit parser or null to remove a previously installed unit parser
     * @return this (for easy method chainging)
     */
    public Eval setUnitParser(final QuantityParser unitParser)
    {
        this.userSuppliedUnitParser = unitParser;
        return this;
    }

    /**
     * Evaluate a mathematical expression
     * @param expression the expression
     * @return The value of the evaluated expression
     * @throws RuntimeException when the expression cannot be evaluated
     */
    public Object evaluate(final String expression) throws RuntimeException
    {
        return evaluateExpression(expression);
    }

    /**
     * Evaluate a mathematical expression, check that the result is a logical value and return that value as a Boolean
     * @param expression the expression
     * @return the result of the expression
     * @throws RuntimeException when the expression could not be evaluated, or the result is not a logical value
     */
    public Boolean evaluateAsBoolean(final String expression) throws RuntimeException
    {
        Object result = evaluateExpression(expression);
        if (!(result instanceof Boolean))
        {
            throwException("Result " + result + " can not be cast to a Boolean");
        }
        return ((Boolean) result);
    }

    /**
     * Evaluate a mathematical expression, check that the result is a floating point value and return that value as a double. If
     * the result is strongly typed (some DJUNITS quantity), the SI value is returned.
     * @param expression the expression
     * @return the result of the expression
     * @throws RuntimeException when the expression could not be evaluated, or the result is not a double value
     */
    public double evaluateAsDouble(final String expression) throws RuntimeException
    {
        Object result = evaluateExpression(expression);
        if (!(result instanceof Quantity<?>))
        {
            throwException("Result " + result + " can not be cast to a double");
        }
        return ((Quantity<?>) result).si;
    }

    /**
     * Create and return a collection of all built in functions.
     * @return all built in functions
     */
    public Collection<Function> builtInFunctions()
    {
        return this.functionData.values();
    }

    /**
     * Install a map of user-defined functions. If a built-in function has the same name as a user-defined function; the
     * user-defined function takes precedence.
     * @param userDefinedFunctionMap map that maps the name of the function to a Function object
     * @return this (for easy method chaining)
     */
    public Eval setUserDefinedFunctions(final Map<String, Function> userDefinedFunctionMap)
    {
        this.userDefinedFunctions = null == userDefinedFunctionMap ? null : new HashMap<>(userDefinedFunctionMap);
        return this;
    }

    /**
     * Evaluate one expression.
     * @param expression the expression to evaluate
     * @return the result of the evaluation (Quantity or Boolean)
     * @throws RuntimeException when the expression could not be evaluated, or the result is not a logical value
     */
    public Object evaluateExpression(final String expression) throws RuntimeException
    {
        Throw.whenNull(expression, "expression may not be null");
        Throw.when(expression.length() == 0, IllegalArgumentException.class, "Expression may not be the empty string");
        String savedExpression = this.expression;
        int savedPosition = this.position;
        List<Object> savedStack = new ArrayList<>(this.stack);
        try
        {
            this.expression = expression;
            this.position = 0;
            this.stack.clear();
            eatSpace();
            evalLhs(0);
            if (this.position < this.expression.length())
            {
                this.throwException("Trailing garbage: \"" + this.expression.substring(this.position) + "\"");
            }
            if (this.stack.size() > 1)
            {
                this.throwException("Unfinished operations");
            }
            if (this.stack.size() <= 0)
            {
                this.throwException("No result after evaluation");
            }
            return pop();
        }
        finally
        {
            this.expression = savedExpression;
            this.position = savedPosition;
            this.stack = savedStack;
        }
    }

    /**
     * Increment position up to the next non-space character.
     */
    private void eatSpace()
    {
        while (this.position < this.expression.length() && Character.isWhitespace(this.expression.charAt(this.position)))
        {
            this.position++;
        }
    }

    /**
     * Evaluate the left-hand-side of a binary operation.
     * @param bindingStrength the binding strength of a pending binary operation (0 if no binary operation is pending)
     * @throws RuntimeException on error
     */
    private void evalLhs(final int bindingStrength) throws RuntimeException
    {
        eatSpace();
        if (this.position >= this.expression.length())
        {
            throwException("Missing operand");
        }
        char token = this.expression.charAt(this.position);
        switch (token)
        {
            case '-': // Unary minus
            {
                this.position++;
                evalLhs(BIND_UMINUS);
                Object value = pop();
                if (value instanceof Quantity<?>)
                {
                    push(((Quantity<?>) value).negate());
                    break;
                }
                throwException("Cannot apply unary minus on " + value);
                break;
            }

            case '!':
                if (this.position >= this.expression.length() - 1 || '=' != this.expression.charAt(this.position + 1))
                {
                    // unary logical negation
                    this.position++;
                    evalLhs(BIND_UMINUS);
                    Object value = pop();
                    if (value instanceof Boolean)
                    {
                        push(!((Boolean) value));
                        break;
                    }
                    throwException("Cannot apply unary not operator on " + value);
                }
                break; // We should not get a "!=" operation here, but if we do, it results in an error later on

            case '(': // parenthesized expression
                this.position++;
                evalLhs(0);
                if (this.position >= this.expression.length() || ')' != this.expression.charAt(this.position))
                {
                    throwException("Missing closing parenthesis");
                }
                this.position++; // step over the closing parenthesis
                break;

            default: // parse one operand
            {
                if (Character.isLetter(token) || '#' == token || '@' == token || '_' == token)
                {
                    push(handleFunctionOrVariableOrNamedConstant());
                }
                else if (Character.isDigit(token) || '.' == token)
                {
                    push(handleNumber());
                }
            }
        }
        evalRhs(bindingStrength);
    }

    /**
     * Evaluate the right hand sid of a mathematical expression. Stop at a closing parenthesis, or a binary operator that does
     * not have a higher binding strength than the argument.
     * @param bindingStrength if the next token is a binary operator with a lower or equal binding strength; return without
     *            consuming that token.
     * @throws RuntimeException when the expression is mathematically unsound
     */
    private void evalRhs(final int bindingStrength) throws RuntimeException
    {
        eatSpace();
        while (this.position < this.expression.length())
        {
            char token = this.expression.charAt(this.position);
            switch (token)
            {
                case ')':
                    return;

                case '^': // power operator
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength > BIND_POW) // Not >=; this ensures a^b^c is evaluated as a^(b^c); not (a^b)^c
                    { // See https://en.wikipedia.org/wiki/Order_of_operations#Serial_exponentiation
                        return; // Cannot happen
                    }
                    this.position++;
                    evalLhs(BIND_POW);
                    power();
                    break;

                case '*':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength >= BIND_MUL)
                    {
                        return;
                    }
                    this.position++;
                    evalLhs(BIND_MUL);
                    multiply();
                    break;

                case '/':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength >= BIND_MUL)
                    {
                        return;
                    }
                    this.position++;
                    evalLhs(BIND_MUL);
                    divide();
                    break;

                case '+':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength >= BIND_ADD)
                    {
                        return;
                    }
                    this.position++;
                    evalLhs(BIND_ADD);
                    add();
                    break;

                case '-':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand"); // Cannot happen; that minus would be considered unary
                    }
                    if (bindingStrength >= BIND_ADD)
                    {
                        return;
                    }
                    this.position++;
                    evalLhs(BIND_ADD);
                    subtract();
                    break;

                case '&': // boolean and with something
                    if (this.position >= this.expression.length() - 1 || '&' != this.expression.charAt(this.position + 1))
                    {
                        throwException("Single \'&\' is not a valid operator");
                    }
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength >= BIND_AND)
                    {
                        return;
                    }
                    this.position += 2;
                    evalLhs(BIND_AND);
                    and();
                    break;

                case '|': // boolean or with something
                    if (this.position >= this.expression.length() - 1 || '|' != this.expression.charAt(this.position + 1))
                    {
                        throwException("Single \'|\' is not a valid operator");
                    }
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    if (bindingStrength >= BIND_OR)
                    {
                        return;
                    }
                    this.position += 2;
                    evalLhs(BIND_OR);
                    or();
                    break;

                case '<':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    this.position++;
                    if (this.position < this.expression.length() && '=' == this.expression.charAt(this.position))
                    {
                        this.position++;
                        evalLhs(BIND_RELATIONAL);
                        compareQuantitys((a, b) -> (a <= b));
                    }
                    else
                    {
                        evalLhs(BIND_RELATIONAL);
                        compareQuantitys((a, b) -> (a < b));
                    }
                    break;

                case '>':
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    this.position++;
                    if (this.position < this.expression.length() && '=' == this.expression.charAt(this.position))
                    {
                        this.position++;
                        evalLhs(BIND_RELATIONAL);
                        compareQuantitys((a, b) -> (a >= b));
                    }
                    else
                    {
                        evalLhs(BIND_RELATIONAL);
                        compareQuantitys((a, b) -> (a > b));
                    }
                    break;

                case '=':
                {
                    if (this.position >= this.expression.length() - 1 || '=' != this.expression.charAt(this.position + 1))
                    {
                        throwException("Single \'=\' is not a valid operator");
                    }
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    this.position += 2;
                    evalLhs(BIND_EQUAL);
                    Object right = pop();
                    Object left = pop();
                    push(left.equals(right)); // This also works for Boolean operands.
                    break;
                }

                case '!':
                {
                    if (this.position >= this.expression.length() - 1 || '=' != this.expression.charAt(this.position + 1))
                    {
                        throwException("Single \'!\' is not a valid operator");
                    }
                    if (this.stack.isEmpty())
                    {
                        throwException("Missing left operand");
                    }
                    this.position += 2;
                    evalLhs(BIND_EQUAL);
                    Object right = pop();
                    Object left = pop();
                    push(!left.equals(right)); // This also works for Boolean operands and mixed-type operands.
                    break;
                }

                case '?': // Conditional expression
                {
                    if (bindingStrength >= BIND_CONDITIONAL_EXPRESSION)
                    {
                        return;
                    }
                    this.position++;
                    // This is special as we really do not want to evaluate or cause side effects on the non-taken part
                    Object choice = pop();
                    if (!(choice instanceof Boolean))
                    {
                        throwException("Condition does not evaluate to a logical value");
                    }
                    if (!((Boolean) choice))
                    {
                        // Skip the "then" part
                        skip(true);
                    }
                    else
                    {
                        evalLhs(0); // should consume everything up to the ':'
                    }
                    if (this.position >= this.expression.length() || ':' != this.expression.charAt(this.position))
                    {
                        throwException("Missing \':\' of conditional expression");
                    }
                    this.position++; // skip the ':'
                    if ((Boolean) choice)
                    {
                        // Skip the "else" part
                        skip(false);
                    }
                    else
                    {
                        evalLhs(BIND_CONDITIONAL_EXPRESSION);
                    }
                    break;
                }

                case ':':
                    return;

                case ',':
                    return;

                default:
                    throwException("Operator expected (got \"" + token + "\")");

            }
        }
    }

    /**
     * Interface for comparator for two double values.
     */
    interface CompareValues
    {
        /**
         * Compare two double values.
         * @param argument1 the left argument of the comparator
         * @param argument2 the right argument of the comparator
         * @return the result type of the function
         */
        Object execute(double argument1, double argument2);

    }

    /**
     * Pop two operands from the stack and compare them using the provided comparator lambda expression
     * @param comparator a function that compares two Quantity values.
     */
    private void compareQuantitys(final CompareValues comparator)
    {
        Object right = pop();
        Object left = pop();
        if ((left instanceof Quantity) && (right instanceof Quantity)
                && getSiUnit((Quantity<?>) left).equals(getSiUnit((Quantity<?>) right)))
        {
            push(comparator.execute(((Quantity<?>) left).si, ((Quantity<?>) right).si));
            return;
        }
        throwException("Cannot compare " + left + " to " + right);
    }

    /**
     * Throw an exception because the expression cannot be evaluated.
     * @param description description of the problem
     * @throws RuntimeException always thrown
     */
    private void throwException(final String description) throws RuntimeException
    {
        throw new RuntimeException(description + " at position " + this.position);
    }

    /**
     * Skip the "then" or the "else" part of a conditional expression.
     * @param thenPart if true; skip the then part (i.e. skip until a ':'); if false; skip the else part (i.e. until end of
     *            expression of a binary operator)
     */
    private void skip(final boolean thenPart)
    {
        while (this.position < this.expression.length())
        {
            switch (this.expression.charAt(this.position))
            {
                case '(': // Eat up everything until the matching closing parenthesis
                {
                    int level = 1;
                    this.position++;
                    while (this.position < this.expression.length())
                    {
                        char c = this.expression.charAt(this.position);
                        if ('(' == c)
                        {
                            level++;
                        }
                        else if (')' == c && --level == 0)
                        {
                            break;
                        }
                        this.position++;
                    }
                    if (level > 0)
                    {
                        throwException("Missing closing parenthesis");
                    }
                    this.position++; // skip the closing parenthesis
                    break;
                }

                case '?': // Nested conditional expression
                    this.position++;
                    skip(true);
                    if (this.position >= this.expression.length() || ':' != this.expression.charAt(this.position))
                    {
                        throwException("Conditional expression missing \':\'");
                    }
                    this.position++;
                    skip(false);
                    break;

                case ':':
                    return;

                case '^':
                case '*':
                case '/':
                case '+':
                case '-':
                case '<':
                case '=':
                case '>':
                case '!':
                    if (!thenPart)
                    {
                        return; // Every operator has lower precedence than the else part
                    }
                    this.position++;
                    break;

                default:
                    this.position++;
                    break;
            }
        }
        if (thenPart)
        {
            throwException("Nonterminated conditional expression");
        }
        // Else end of expression is end of else part
    }

    /**
     * Perform the boolean AND operation on the two top-most elements on the stack and push the result back onto the stack.
     */
    private void and()
    {
        Object right = pop();
        Object left = pop();
        if ((left instanceof Boolean) && (right instanceof Boolean))
        {
            push(((Boolean) left) && ((Boolean) right));
            return;
        }
        throwException("Cannot compute logical AND of " + left + " and " + right);
    }

    /**
     * Perform the boolean OR operation on the two top-most elements on the stack and push the result back onto the stack.
     */
    private void or()
    {
        Object right = pop();
        Object left = pop();
        if ((left instanceof Boolean) && (right instanceof Boolean))
        {
            push(((Boolean) left) || ((Boolean) right));
            return;
        }
        throwException("Cannot compute logical AND of " + left + " and " + right);
    }

    /**
     * Perform the power operation on the two top-most elements on stack and push the result back onto the stack.
     */
    private void power()
    {
        Object exponent = pop();
        Object base = pop();
        push(performPower(base, exponent));
    }

    /**
     * Perform the power operation on the two arguments and return the result
     * @param base the base operand of the power operation
     * @param exponent the exponent of the power operation
     * @return the result of the power operation
     */
    private Object performPower(final Object base, final Object exponent)
    {
        if ((base instanceof Quantity) && (exponent instanceof Quantity)
                && getSiUnit((Quantity<?>) base).equals(getSiUnit(Unitless.BASE))
                && getSiUnit((Quantity<?>) exponent).equals(getSiUnit(Unitless.BASE)))
        {
            var result = Dimensionless.ofSi(Math.pow(((Quantity<?>) base).si, ((Quantity<?>) exponent).si));
            // System.out.println(base + " ^ " + exponent + " = " + result);
            return result;
        }
        throwException("Cannot raise " + base + " to power " + exponent);
        return null; // Not reached
    }

    /**
     * Perform the atan2 function on the two arguments and return the result
     * @param y should be some kind of Quantity
     * @param x should be some kind of Quantity with the same SiDimensions as y
     * @return in fact a Quantity with a quantity matching Dimensionless
     */
    private Object performAtan2(final Object y, final Object x)
    {
        if ((y instanceof Quantity) && (x instanceof Quantity) && getSiUnit((Quantity<?>) y).equals(getSiUnit((Quantity<?>) x)))
        {
            var result = Dimensionless.ofSi(Math.atan2(((Quantity<?>) y).si, ((Quantity<?>) x).si));
            // System.out.println(base + " ^ " + exponent + " = " + result);
            return result;
        }
        throwException("Cannot compute atan2 of " + y + ", " + x + ")");
        return null; // Not reached

    }

    /**
     * Multiply the two top-most elements on the stack and push the result back onto the stack.
     */
    private void multiply()
    {
        Object right = pop();
        Object left = pop();
        if ((right instanceof Quantity) && (left instanceof Quantity))
        {
            push(((Quantity<?>) left).multiply((Quantity<?>) right));
            return;
        }
        throwException("Cannot multiply with " + right + " as right hand operand");
    }

    /**
     * Divide the two top-most elements on the stack and push the result back onto the stack.
     */
    private void divide()
    {
        Object right = pop();
        Object left = pop();
        if ((left instanceof Quantity) && (right instanceof Quantity))
        {
            if (0.0 == ((Quantity<?>) right).si)
            {
                throwException("Division by 0");
            }
            push(((Quantity<?>) left).divide((Quantity<?>) right));
            return;
        }
        throwException("Cannot divide " + left + " by " + right);
    }

    /**
     * Add the two top-most elements on the stack and push the result back onto the stack.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void add()
    {
        Object right = pop();
        Object left = pop();
        if (!(left instanceof Quantity))
        {
            throwException("Left operand of addition must be a scalar (got \"" + left + "\")");
        }
        if (!(right instanceof Quantity))
        {
            throwException("Right operand of addition must be a scalar (got \"" + right + "\")");
        }
        // Both operands are Quantity
        if (!((Quantity<?>) left).siUnit().equals(((Quantity<?>) right).siUnit()))
        {
            // System.out.println("left: " + getDimensions((Quantity<?>) left));
            // System.out.println("right: " + getDimensions((Quantity<?>) right));
            throwException("Cannot add " + left + " to " + right + " because the types are incompatible");
        }

        // Operands are of compatible unit
        // Rel + Rel -> Rel
        var dsl = (Quantity) left;
        var dsr = (Quantity) right;
        var sum = dsl.add(dsr);
        // System.out.println(left + " + " + right + " = " + sum);
        // Set display unit???
        push(sum);
        return;
    }

    /**
     * Subtract the two top-most elements on the stack and push the result back onto the stack.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void subtract()
    {
        Object right = pop();
        Object left = pop();
        if (!(left instanceof Quantity))
        {
            throwException("Left operand of subtraction must be a scalar (got \"" + left + "\")");
        }
        if (!(right instanceof Quantity))
        {
            throwException("Right operand of subtraction must be a scalar (got \"" + right + "\")");
        }
        // Now we know that we're dealing with Quantity objects
        if (!getSiUnit((Quantity<?>) left).equals(getSiUnit((Quantity<?>) right)))
        {
            throwException("Cannot subtract " + right + " from " + left + " because the types are incompatible");
        }
        // Rel - Rel -> Rel
        var dsl = (Quantity) left;
        var dsr = (Quantity) right;
        var difference = dsl.subtract(dsr);
        // System.out.println(left + " - " + right + " = " + difference);
        push(difference);
    }

    /**
     * Parse a number and convert it to a SIQuantity. If it is followed by an SI unit string inside square brackets, parse it
     * into the correct type.
     * @return the value of the parsed number or value
     */
    private Quantity<?> handleNumber()
    {
        // Parse a number value
        int startPosition = this.position;
        boolean seenExp = false;
        boolean seenExpSign = false;
        boolean seenExpDigit = false;
        boolean seenRadix = false;
        while (this.position < this.expression.length())
        {
            char c = this.expression.charAt(this.position);
            // Any leading plus and minus signs at the start of this value are consumed by evalLhs
            // A subsequent plus and minus sign indicates an addition or subtraction
            if (seenExp && (!seenExpDigit) && ('-' == c || '+' == c))
            {
                if (seenExpSign)
                {
                    throwException("Too many signs in exponent");
                }
                seenExpSign = true;
            }
            else if (seenExp && seenExpSign && (!Character.isDigit(c)))
            {
                break;
            }
            else if ('e' == c || 'E' == c)
            {
                if (seenExp)
                {
                    throwException("Too many 'e' or 'E' indicators");
                }
                seenExp = true;
            }
            else if ('.' == c)
            {
                if (seenRadix)
                {
                    throwException("Too many '.'");
                }
                if (seenExp)
                {
                    throwException("A \'.\' is not allowed after \'e\' or \'E\'");
                }
                seenRadix = true;
            }
            else if (Character.isDigit(c))
            {
                if (seenExp)
                {
                    seenExpDigit = true;
                }
            }
            else
            {
                break;
            }
            this.position++;
        }
        eatSpace();
        if (seenExp && !seenExpDigit)
        {
            throwException("Exponent value missing");
        }
        String number = this.expression.substring(startPosition, this.position);
        if (this.position < this.expression.length() && '[' == this.expression.charAt(this.position))
        {
            this.position++;
            startPosition = this.position;
            while (this.position < this.expression.length())
            {
                char c = this.expression.charAt(this.position);
                if (']' == c)
                {
                    String unit = this.expression.substring(startPosition, this.position++);
                    Quantity<?> result = null;
                    if (null != this.userSuppliedUnitParser)
                    {
                        result = this.userSuppliedUnitParser.parseQuantity(Double.parseDouble(number), unit);
                    }
                    if (null == result)
                    {
                        result = SIQuantity.valueOf(number + " " + unit);
                    }
                    return result;
                }
                if ((!Character.isLetterOrDigit(c)) && '-' != c && '^' != c && '/' != c && '.' != c)
                {
                    throwException("Bad symbol in SI unit string");
                }
                this.position++;
            }
            if (this.position >= this.expression.length())
            {
                throwException("Missing closing bracket (\']\')");
            }
        }
        return SIQuantity.valueOf(number); // No unit specified
    }

    /**
     * Process a function call, a variable interpolation, or a mathematical, or physical constant. Precondition: this.position
     * points to the first letter of the name of the function, variable, or constant.
     * @return the value of the function, variable, or mathematical or physical constant
     */
    private Object handleFunctionOrVariableOrNamedConstant()
    {
        int startPosition = this.position;
        while (this.position < this.expression.length())
        {
            char c = this.expression.charAt(this.position);
            if (Character.isLetterOrDigit(c) || '.' == c || '_' == c || '@' == c || '#' == c)
            {
                this.position++;
            }
            else
            {
                break;
            }
        }
        String name = this.expression.substring(startPosition, this.position);
        eatSpace();
        if (this.position >= this.expression.length() || this.expression.charAt(this.position) != '(')
        {
            // No opening parenthesis; name must be the name of a variable; look it up
            Object result = null == this.retrieveValue ? null : this.retrieveValue.lookup(name);
            if (null == result)
            {
                throwException("Cannot resolve variable " + name);
            }
            // Do not check the type of the result to allow expression that return any kind of object
            return result;
        }
        // At an opening parenthesis. This signals the start of a function call; collect the parameters of the function on the
        // stack and count them
        this.position++;
        eatSpace();
        int argCount = 0;
        while (this.position < this.expression.length() && ')' != this.expression.charAt(this.position))
        {
            evalLhs(0);
            argCount++;
            eatSpace();
            if (this.position < this.expression.length() && ',' == this.expression.charAt(this.position))
            {
                this.position++;
            }
        }
        if (this.position >= this.expression.length() || ')' != this.expression.charAt(this.position))
        {
            throwException("Missing closing parenthesis");
        }
        this.position++; // step over the closing parenthesis
        Function f = null;
        if (null != this.userDefinedFunctions)
        {
            f = this.userDefinedFunctions.get(name);
        }
        if (null == f)
        {
            f = this.functionData.get(name);
        }
        if (null == f)
        {
            throwException("Unknown function " + name);
        }
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++)
        {
            args[argCount - i - 1] = pop();
        }
        if (f.getMetaData() != MetaData.NO_META_DATA)
        {
            // MetaData.verifyComposition does not handle this case correctly.
            int argsNeeded = f.getMetaData().getObjectDescriptors().length;
            if (argsNeeded != argCount)
            {
                throwException(name + " needs " + argsNeeded + " parameter" + (argsNeeded == 1 ? "" : "s") + " (got " + argCount
                        + ")");
            }
            for (int i = 0; i < argCount; i++)
            {
                if ((args[i] instanceof Boolean) && (!Boolean.class.isAssignableFrom(f.getMetaData().getObjectClass(i))))
                {
                    throwException(name + " does not take " + args[i] + " as parameter " + i);
                }
                else if ((args[i] instanceof Quantity) && (Quantity.class.isAssignableFrom(f.getMetaData().getObjectClass(i))))
                {
                    Quantity<?> ds = (Quantity<?>) args[i];
                    Class<?> clazz = f.getMetaData().getObjectClass(i);
                    if (!clazz.equals(Quantity.class))
                    {
                        SIUnit siUnits = this.siUnitsMap.get(clazz);
                        if (null == siUnits)
                        {
                            // Not in the cache
                            try
                            {
                                Field field = clazz.getDeclaredField("ZERO"); // Every Quantity type has this
                                Quantity<?> zero = (Quantity<?>) field.get(clazz);
                                siUnits = zero.siUnit();
                                this.siUnitsMap.put(clazz, siUnits); // Add this one to our map
                            }
                            catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException nsfe)
                            {
                                throwException("ERROR: Cannot determine quantity for " + clazz.getCanonicalName());
                            }
                        }
                        if (!siUnits.equals(getSiUnit(ds)))
                        {
                            throwException("parameter " + i + " of " + name + " has incompatible quantity");
                        }
                    }
                }
                else
                {
                    throwException("Argument " + i + " of function " + name + " is of an unhandled type (" + args[i] + ")");
                }
            }
        }
        // All parameters are apparently compatible; invoke the function
        return (f.function(args));
    }

    /**
     * Push one object onto the evaluation stack.
     * @param object the object to push onto the evaluation stack
     */
    private void push(final Object object)
    {
        this.stack.add(object);
    }

    /**
     * Pop one object from the evaluation stack. Throw exception when stack underflows
     * @return the object popped from the evaluation stack
     * @throws RuntimeException when the stack is currently empty
     */
    private Object pop() throws RuntimeException
    {
        if (this.stack.isEmpty())
        {
            throwException("Stack empty");
        }
        return (this.stack.remove(this.stack.size() - 1));
    }

    /**
     * Convert an object to a Dimensionless if possible, or complain.
     * @param functionData meta data of the function that wants a Dimensionless
     * @param object object that supposedly can be converted to a Dimensionless
     * @return the result
     */
    private Dimensionless checkDimensionless(final Function functionData, final Object object)
    {
        if (!(object instanceof Quantity))
        {
            throwException("Function " + functionData.getId() + " cannot be applied to " + object);
        }
        Quantity<?> ds = (Quantity<?>) object;
        if (!getSiUnit(ds).equals(getSiUnit(Unitless.BASE)))
        {
            throwException("Function " + functionData.getId() + " cannot be applied to " + ds);
        }
        return Dimensionless.ofSi(ds.si);
    }

    /**
     * Retrieve the SIUnit of a Quantity.
     * @param quantity the Quantity
     * @return the SIUnit object that describes the SI unit of the Quantity
     */
    private static SIUnit getSiUnit(final Quantity<?> quantity)
    {
        return quantity.siUnit();
    }

    /**
     * Retrieve the SIUnit of a unit.
     * @param unit the unit
     * @return the SIUnit unit
     */
    private static SIUnit getSiUnit(final UnitInterface<?> unit)
    {
        return unit.siUnit();
    }

}
