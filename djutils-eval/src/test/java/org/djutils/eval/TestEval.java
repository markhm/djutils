package org.djutils.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.djunits.quantity.Dimensionless;
import org.djunits.quantity.Duration;
import org.djunits.quantity.Force;
import org.djunits.quantity.Speed;
import org.djunits.quantity.def.Quantity;
import org.djunits.unit.Unitless;
import org.djunits.unit.si.SIUnit;
import org.djunits.util.Constants;
import org.djutils.metadata.MetaData;
import org.djutils.metadata.ObjectDescriptor;
import org.junit.jupiter.api.Test;

/**
 * TestEval.java.
 * <p>
 * Copyright (c) 2023-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Peter Knoppers
 */
public class TestEval
{
    /**
     * Test some basic things
     */
    @Test
    public void testBasics()
    {
        try
        {
            new Eval().evaluate(null);
            fail("null pointer for expression should have thrown a NullPointerException");
        }
        catch (NullPointerException npe)
        {
            // Ignore expected exception
        }

        try
        {
            new Eval().evaluate("");
            fail("empty string for expression should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
            // Ignore expected exception
        }

        try
        {
            new Eval().evaluate("123)"); // Unbalanced closing parentheses
            fail("Unbalanced closing parentheses should have thrown a RunT=timeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().contains("position 3"), "Message describes position of problem");
        }

        // Unary operators with no argument
        for (String operation : new String[] {"-", "!"})
        {
            try
            {
                new Eval().evaluate(operation);
                fail("Unary minus with no trailing operand should have thrown a RuntimException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("missing operand"), "Message describes the problem");
            }
        }

        // Binary operators
        for (String operation : new String[] {"^", "*", "/", "+", "&&", "||", "<", "<=", ">", ">=", "==", "!="})
        {
            // With both operands missing
            try
            {
                new Eval().evaluate(operation);
                fail("Binary operand without operands should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("missing left operand"), "Message describes the problem");
            }

            // With left operand, but missing right operand
            try
            {
                new Eval().evaluate("123" + operation);
                fail("Missing right operand should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("missing operand"), "Message describes the problem");
            }
        }
        try
        {
            new Eval().evaluate("123-");
            fail("Missing right operand should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing operand"), "Message describes the problem");
        }

        for (String operation : new String[] {"&", "|"})
        {
            try
            {
                new Eval().evaluate(operation);
                fail("Invalid partial operator should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("not a valid operator"), "Message describes the problem");
            }
        }

        try
        {
            new Eval().evaluate("-TRUE()");
            fail("Unary minus cannot be applied to logical value");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot apply unary minus"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()&");
            fail("Incomplete operator should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'&\' is not a valid operator"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()&TRUE()");
            fail("Incomplete operator should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'&\' is not a valid operator"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()|");
            fail("Incomplete operator should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'|\' is not a valid operator"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()|TRUE()");
            fail("Incomplete operator should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'|\' is not a valid operator"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("(123+456");
            fail("Unclosed parenthesis should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing closing parenthesis"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("(123+456(");
            fail("Unclosed parenthesis should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("operator expected"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("+123");
            fail("Double (unary) plus should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing left operand"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("-+123");
            fail("Double (unary) plus should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing left operand"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123 [m/s");
            fail("Missing closing bracket of unit should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing closing bracket (\']\')"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123 [m/s*");
            fail("Missing closing bracket of unit should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("bad symbol in si unit string"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("abc+123");
            fail("Variable without RetrieveValue object should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot resolve variable "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("abc.def_pqr+123");
            fail("Variable without RetrieveValue object should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot resolve variable "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("abc");
            fail("Variable without RetrieveValue object should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot resolve variable "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("abc(");
            fail("Variable without RetrieveValue object should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing closing parenthesis "),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123+456%");
            fail("Bad character where operator is expected should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("operator expected "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("cos()");
            fail("Too few arguments should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("needs 1 "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("cos(1, 2)");
            fail("Too many arguments should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("needs 1 "), "Message describes the problem");
        }

        verifyValueAndUnit("dot in unit", new Eval().evaluateExpression("20[kg.m/s2]"), 20, 0, Force.ONE);
    }

    /**
     * Test all binary operations.
     */
    @Test
    public void testBinaryOperations()
    {
        verifyValueAndUnit("Dimensionless + Dimensionless", new Eval().evaluate("123+456"), 579, 0, Dimensionless.ONE);
        verifyValueAndUnit("Dimensionless - Dimensionless", new Eval().evaluate("123-456"), -333, 0, Dimensionless.ONE);
        verifyValueAndUnit("Dimensionless * Dimensionless", new Eval().evaluate("123*456"), 56088, 0, Dimensionless.ONE);
        verifyValueAndUnit("Dimensionless / Dimensionless", new Eval().evaluate("123/456"), 123.0 / 456, 0.00001,
                Dimensionless.ONE);
        verifyValueAndUnit("Dimensionless ^ Dimensionless", new Eval().evaluate("123^4.56"), Math.pow(123, 4.56), 0.1,
                Dimensionless.ONE);
        verifyBoolean("Dimensionless > Dimensionless", new Eval().evaluate("123>456"), false);
        verifyBoolean("Dimensionless > Dimensionless", new Eval().evaluate("456>123"), true);
        verifyBoolean("Dimensionless >= Dimensionless", new Eval().evaluate("123>=123"), true);
        verifyBoolean("Dimensionless >= Dimensionless", new Eval().evaluate("123>=123.01"), false);
        verifyBoolean("Dimensionless < Dimensionless", new Eval().evaluate("123<456"), true);
        verifyBoolean("Dimensionless < Dimensionless", new Eval().evaluate("456<123"), false);
        verifyBoolean("Dimensionless <= Dimensionless", new Eval().evaluate("123<=123"), true);
        verifyBoolean("Dimensionless <= Dimensionless", new Eval().evaluate("123<=122.99"), false);
        verifyBoolean("Dimensionless == Dimensionless", new Eval().evaluate("123==123"), true);
        verifyBoolean("Dimensionless == Dimensionless", new Eval().evaluate("123==124"), false);
        verifyBoolean("Dimensionless != Dimensionless", new Eval().evaluate("123!=123"), false);
        verifyBoolean("Dimensionless != Dimensionless", new Eval().evaluate("123!=124"), true);
        verifyBoolean("not operator", new Eval().evaluate("!2==2"), false);
        verifyBoolean("not operator", new Eval().evaluate("!2!=2"), true);
        verifyBoolean("not operator", new Eval().evaluate("!3==3"), false);
        verifyBoolean("not operator", new Eval().evaluate("!3!=3"), true);
        verifyBoolean("true && true", new Eval().evaluate("TRUE()&&TRUE()"), true);
        verifyBoolean("true && false", new Eval().evaluate("TRUE()&&FALSE()"), false);
        verifyBoolean("false && true", new Eval().evaluate("FALSE()&&TRUE()"), false);
        verifyBoolean("false && false", new Eval().evaluate("FALSE()&&FALSE()"), false);
        verifyBoolean("true || true", new Eval().evaluate("TRUE()||TRUE()"), true);
        verifyBoolean("true || false", new Eval().evaluate("TRUE()||FALSE()"), true);
        verifyBoolean("false || true", new Eval().evaluate("FALSE()||TRUE()"), true);
        verifyBoolean("false || false", new Eval().evaluate("FALSE()||FALSE()"), false);
        for (String operator : new String[] {"^", "*", "/", "+", "-", "<", "<=", ">", ">=", "&&", "||"})
        {
            try
            {
                new Eval().evaluate("123" + operator + "TRUE()");
                fail("Illegal operand type should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("right operand ")
                        || rte.getMessage().toLowerCase().contains("cannot "), "Message is descriptive");
            }

            try
            {
                new Eval().evaluate("TRUE()" + operator + "123");
                fail("Illegal operand type should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("left operand ")
                        || rte.getMessage().toLowerCase().contains("cannot "), "Message is descriptive");
            }

        }

        for (String operator : new String[] {"^", "+", "-", "<", "<=", ">", ">=", "&&", "||"})
        {
            try
            {
                new Eval().evaluate("123" + operator + "345 [m/s]");
                fail("Non-compatible operand types should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("cannot "), "Message describes the problem");
            }
        }
    }

    /**
     * Test some illegal operators
     */
    @Test
    public void testIllegalOperators()
    {
        try
        {
            new Eval().evaluate("2=5");
            fail("Single \'=\' should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'=\' is not a valid operator"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()!FALSE()");
            fail("Single \'!\' should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("single \'!\' is not a valid operator"),
                    "Message describes the problem");
        }

    }

    /**
     * Test handling of binary operator at the end of the expression.
     */
    @Test
    public void testBinaryOperatorAtEndOfExpression()
    {
        for (String operation : new String[] {"^", "*", "/", "+", "&&", "||", "<", "<=", ">", ">=", "==", "!="})
        {
            try
            {
                new Eval().evaluate("123" + operation);
                fail("Binary operator at end of expression should have thrown a RuntimeException");
            }
            catch (RuntimeException rte)
            {
                assertTrue(rte.getMessage().toLowerCase().contains("missing operand"), "Message describes the problem");
            }
        }
    }

    /**
     * Test the number parser
     */
    @Test
    public void testNumberParser()
    {
        verifyValueAndUnit("E notation", new Eval().evaluate("2E6"), 2e6, 0, Dimensionless.ONE);
        verifyValueAndUnit("E notation", new Eval().evaluate("2E-6"), 2e-6, 0, Dimensionless.ONE);
        verifyValueAndUnit("e notation", new Eval().evaluate("2e6"), 2e6, 0, Dimensionless.ONE);
        verifyValueAndUnit("e notation", new Eval().evaluate("2e-6"), 2e-6, 0, Dimensionless.ONE);
        verifyValueAndUnit("E notation", new Eval().evaluate("2E+6"), 2e6, 0, Dimensionless.ONE);
        verifyValueAndUnit("e notation", new Eval().evaluate("2e+6"), 2e6, 0, Dimensionless.ONE);
        try
        {
            new Eval().evaluate("123e45e6");
            fail("Multiple e letters in a number should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("too many"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123e4.5");
            fail("Decimal symbol after e should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("not allowed after"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123.456.789");
            fail("Multiple radix symbols should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("too many"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123e--4");
            fail("Multiple minus signs in exponent should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("too many"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123e-+4");
            fail("Multiple signs in exponent should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("too many"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123..56");
            fail("Multiple radix symbols in mantissa should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("too many"), "Message describes the problem");
        }

    }

    /**
     * Test the zero argument functions.
     */
    @Test
    public void testZeroArgumentFunctions()
    {
        try
        {
            new Eval().evaluate("NONEXISTENTZEROARGUMENTFUNCTION()");
            fail("Calling a non existent zero-argument function should have thrown a RunTimeException");
        }
        catch (RuntimeException re)
        {
            assertTrue(re.getMessage().contains("Unknown"), "exception is descriptive");
        }
        verifyValueAndUnit("Avogadro constant", new Eval().evaluate("AVOGADRO()"), Constants.AVOGADRO.si, 0.0,
                Constants.AVOGADRO);
        verifyValueAndUnit("Boltzmann constant", new Eval().evaluate("BOLTZMANN()"), Constants.BOLTZMANN.si, 0.0,
                Constants.BOLTZMANN);
        verifyValueAndUnit("Cesium 133 frequency constant", new Eval().evaluate("CESIUM133_FREQUENCY()"),
                Constants.CESIUM133_FREQUENCY.si, 0.0, Constants.CESIUM133_FREQUENCY);
        verifyValueAndUnit("Base of natural logarithm", new Eval().evaluate("E()"), Constants.E.si, 0.0, Constants.E);
        verifyValueAndUnit("Electrical charge of an electron", new Eval().evaluate("ELECTRONCHARGE()"),
                Constants.ELECTRONCHARGE.si, 0.0, Constants.ELECTRONCHARGE);
        verifyValueAndUnit("Mass of an electrong", new Eval().evaluate("ELECTRONMASS()"), Constants.ELECTRONMASS.si, 0.0,
                Constants.ELECTRONMASS);
        verifyValueAndUnit("Gravitational constant at sea level", new Eval().evaluate("G()"), Constants.G.si, 0.0, Constants.G);
        verifyValueAndUnit("Speed of light in vacuum", new Eval().evaluate("LIGHTSPEED()"), Constants.LIGHTSPEED.si, 0.0,
                Constants.LIGHTSPEED);
        verifyValueAndUnit("Luminous efficacy Kcd of monochromatic radiation of frequency 540×10^12 Hz (540 THz). ",
                new Eval().evaluate("LUMINOUS_EFFICACY_540THZ()"), Constants.LUMINOUS_EFFICACY_540THZ.si, 0.0,
                Constants.LUMINOUS_EFFICACY_540THZ);
        verifyValueAndUnit("Mass of a neutron", new Eval().evaluate("NEUTRONMASS()"), Constants.NEUTRONMASS.si, 0.0,
                Constants.NEUTRONMASS);
        verifyValueAndUnit("Verify value of PI", new Eval().evaluate("PI()"), Constants.PI.si, 0.0, Constants.PI);
        verifyValueAndUnit("Phi (the golden ratio)", new Eval().evaluate("PHI()"), Constants.PHI.si, 0.0, Constants.PHI);
        verifyValueAndUnit("Planck constant", new Eval().evaluate("PLANCK()"), Constants.PLANCK.si, 0.0, Constants.PLANCK);
        verifyValueAndUnit("Planck constant divided by 2 pi", new Eval().evaluate("PLANCKREDUCED()"),
                Constants.PLANCKREDUCED.si, 0.0, Constants.PLANCKREDUCED);
        verifyValueAndUnit("Electrical charge of a proton", new Eval().evaluate("PROTONCHARGE()"), Constants.PROTONCHARGE.si,
                0.0, Constants.PROTONCHARGE);
        verifyValueAndUnit("Mass of a proton", new Eval().evaluate("PROTONMASS()"), Constants.PROTONMASS.si, 0.0,
                Constants.PROTONMASS);
        verifyValueAndUnit("Tau (2 * pi)", new Eval().evaluate("TAU()"), Constants.TAU.si, 0.0, Constants.TAU);
        verifyValueAndUnit("Impedance of vacuum", new Eval().evaluate("VACUUMIMPEDANCE()"), Constants.VACUUMIMPEDANCE.si, 0.0,
                Constants.VACUUMIMPEDANCE);
        verifyValueAndUnit("Permeability of vacuum", new Eval().evaluate("VACUUMPERMEABILITY()"),
                Constants.VACUUMPERMEABILITY.si, 0.0, Constants.VACUUMPERMEABILITY);
        verifyValueAndUnit("Permittivity of vacuum", new Eval().evaluate("VACUUMPERMITTIVITY()"),
                Constants.VACUUMPERMITTIVITY.si, 0.0, Constants.VACUUMPERMITTIVITY);
        verifyBoolean("Logical value true", new Eval().evaluate("TRUE()"), true);
        verifyBoolean("Logical value false", new Eval().evaluate("FALSE()"), false);
        verifyValueAndUnit("Number that starts with radix symbol", new Eval().evaluate(".345"), .345, 0, Dimensionless.ONE);
    }

    /**
     * Test the unary operators
     */
    @Test
    public void testUnaryOperators()
    {
        try
        {
            new Eval().evaluate("!5");
            fail("Applying logical not on non-logical value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot apply unary not operator"), "Message is descriptive");
        }

        try
        {
            new Eval().evaluate("123 456"); // Missing binary operator
            fail("Two operands with no operator between them should have thown a RuntimException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("operator expected"), "Message is descriptive");
        }

        try
        {
            new Eval().evaluate("-TRUE()"); // Unary minus on logical value
            fail("Unary minus on logical value should have thrown an RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot apply unary minus"), "Message is descriptive");
        }

        try
        {
            new Eval().evaluate("!5"); // Logical not operator on non logical operand
            fail("Unary not operator on non logical operand should have thrown an RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot apply unary not"), "Message is descriptive");
        }
    }

    /**
     * Test the one argument functions (mostly math)
     */
    @Test
    public void testOneArgumentFunctions()
    {
        try
        {
            new Eval().evaluate("nosuchoneargumentfunction(123)");
            fail("Nonexistant one-argument function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function"), "Message describes the problem");
        }
        try
        {
            new Eval().evaluate("sin(TRUE())");
            fail("Math function called on logical value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("does not take "), "Message describes the problem");
        }
        try
        {
            new Eval().evaluate("sin(123 [m])");
            fail("Math function called on non-dimensionless value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("incompatible quantity"), "Message describes the problem");
        }
        verifyValueAndUnit("acos(-1)", new Eval().evaluate("acos(-1)"), Math.acos(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("acos(0.5)", new Eval().evaluate("acos(0.5)"), Math.acos(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("asin(-1)", new Eval().evaluate("asin(-1)"), Math.asin(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("asin(0.5)", new Eval().evaluate("asin(0.5)"), Math.asin(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("atan(-1)", new Eval().evaluate("atan(-1)"), Math.atan(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("atan(0.5)", new Eval().evaluate("atan(0.5)"), Math.atan(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cbrt(50)", new Eval().evaluate("cbrt(50)"), Math.cbrt(50), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cbrt(0.5)", new Eval().evaluate("cbrt(0.5)"), Math.cbrt(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cos(-1)", new Eval().evaluate("cos(-1)"), Math.cos(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cos(0.5)", new Eval().evaluate("cos(0.5)"), Math.cos(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cosh(-1)", new Eval().evaluate("cosh(-1)"), Math.cosh(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("cosh(0.5)", new Eval().evaluate("cosh(0.5)"), Math.cosh(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("exp(-1)", new Eval().evaluate("exp(-1)"), Math.exp(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("exp(0.5)", new Eval().evaluate("exp(0.5)"), Math.exp(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("expm1(-1)", new Eval().evaluate("expm1(-1)"), Math.expm1(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("expm1(0.5)", new Eval().evaluate("expm1(0.5)"), Math.expm1(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log(50)", new Eval().evaluate("log(50)"), Math.log(50), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log(0.5)", new Eval().evaluate("log(0.5)"), Math.log(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log10(50)", new Eval().evaluate("log10(50)"), Math.log10(50), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log10(0.5)", new Eval().evaluate("log10(0.5)"), Math.log10(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log1p(50)", new Eval().evaluate("log1p(50)"), Math.log1p(50), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("log1p(0.5)", new Eval().evaluate("log1p(0.5)"), Math.log1p(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("signum(0)", new Eval().evaluate("signum(0)"), Math.signum(0), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("signum(0.5)", new Eval().evaluate("signum(0.5)"), Math.signum(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("signum(-0.5)", new Eval().evaluate("signum(-0.5)"), Math.signum(-0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sin(-1)", new Eval().evaluate("sin(-1)"), Math.sin(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sin(0.5)", new Eval().evaluate("sin(0.5)"), Math.sin(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sinh(-1)", new Eval().evaluate("sinh(-1)"), Math.sinh(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sinh(0.5)", new Eval().evaluate("sinh(0.5)"), Math.sinh(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sqrt(50)", new Eval().evaluate("sqrt(50)"), Math.sqrt(50), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("sqrt(0.5)", new Eval().evaluate("sqrt(0.5)"), Math.sqrt(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("tan(-1)", new Eval().evaluate("tan(-1)"), Math.tan(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("tan(0.5)", new Eval().evaluate("tan(0.5)"), Math.tan(0.5), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("tanh(-1)", new Eval().evaluate("tanh(-1)"), Math.tanh(-1), 0.000001, Dimensionless.ONE);
        verifyValueAndUnit("tanh(0.5)", new Eval().evaluate("tanh(0.5)"), Math.tanh(0.5), 0.000001, Dimensionless.ONE);
    }

    /**
     * Test the two-parameter functions.
     */
    @Test
    public void testTwoArgumentFunctions()
    {
        verifyValueAndUnit("pow(3.4,5.2)", new Eval().evaluate("pow(3.4,5.2)"), Math.pow(3.4, 5.2), 0.1, Dimensionless.ONE);

        try
        {
            new Eval().evaluate("12[m]^3");
            fail("Power operator on non-dimensionless should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot raise "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("pow(TRUE(),5.2)");
            fail("Attempt to raise a logical value to some power should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("does not take "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("pow(3.4,TRUE())");
            fail("Attempt to raise a value to a logical value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("does not take "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("pow(3.4,5.2[m])");
            fail("Attempt to raise a value to a non Dimensionless value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("incompatible quantity"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("pow(3.4[s],5.2)");
            fail("Attempt to raise a value to a non Dimensionless value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("incompatible quantity"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()^5.2");
            fail("Attempt to raise a logical value to some power should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot raise "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("3.4^TRUE()");
            fail("Attempt to raise a value to a logical value should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot raise "), "Message describes the problem");
        }

        verifyValueAndUnit("atan2(1,2)", new Eval().evaluate("atan2(1,2)"), Math.atan2(1, 2), 0.00001, Dimensionless.ONE);
        verifyValueAndUnit("atan2(-2,-1)", new Eval().evaluate("atan2(-2,-1)"), Math.atan2(-2, -1), 0.00001, Dimensionless.ONE);
        try
        {
            new Eval().evaluate("atan2(TRUE(),1)");
            fail("Attempt to use a logical operand in atan2 should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("atan2 does not take "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("atan2(2,FALSE())");
            fail("Attempt to use a logical operand in atan2 should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("atan2 does not take "), "Message describes the problem");
        }

        verifyValueAndUnit("atan2(1[m],2[m])", new Eval().evaluate("atan2(1[m],2[m])"), Math.atan2(1, 2), 0.00001,
                Dimensionless.ONE);

        try
        {
            new Eval().evaluate("atan2(1[m],2[s])");
            fail("Attempt to use atan2 with parameters of different types should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot compute atan2 of "), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("nosuchtwoparameterfunction(123,456)");
            fail("Attempt to call an non-existant two-parameter function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function "), "Message describes the problem");
        }
    }

    /**
     * Test division by zero
     */
    @Test
    public void testDivisionByZero()
    {
        try
        {
            new Eval().evaluate("1/0");
            fail("Division by zero should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("division by 0"), "Message describes the problem");
        }
    }

    /**
     * Test the order in which operators are applied.
     */
    @Test
    public void testEvaluationOrder()
    {
        verifyValueAndUnit("13+17-19+23-31", new Eval().evaluate("13+17-19+23-31"), 13 + 17 - 19 + 23 - 31, 0,
                Dimensionless.ONE);
        verifyValueAndUnit("13+17-19/23-31", new Eval().evaluate("13+17-19/23-31"), 13 + 17 - 19.0 / 23 - 31, 0,
                Dimensionless.ONE);
        verifyValueAndUnit("13+17-19/23^3-31", new Eval().evaluate("13+17-19/23^3-31"), 13 + 17 - 19.0 / Math.pow(23, 3) - 31,
                0.0000001, Dimensionless.ONE);
        verifyValueAndUnit("13*17/19*23/31", new Eval().evaluate("13*17/19*23/31"), 13.0 * 17 / 19 * 23 / 31, 0.000001,
                Dimensionless.ONE);
        verifyBoolean("TRUE()&&TRUE()&&TRUE()", new Eval().evaluate("TRUE()&&TRUE()&&TRUE()"), true);
        verifyBoolean("TRUE()&&TRUE()&&FALSE()", new Eval().evaluate("TRUE()&&TRUE()&&FALSE()"), false);
        verifyBoolean("TRUE()&&FALSE()&&TRUE()", new Eval().evaluate("TRUE()&&FALSE()&&TRUE()"), false);
        verifyBoolean("TRUE()&&FALSE()&&FALSE()", new Eval().evaluate("TRUE()&&FALSE()&&FALSE()"), false);
        verifyBoolean("FALSE()&&TRUE()&&TRUE()", new Eval().evaluate("FALSE()&&TRUE()&&TRUE()"), false);
        verifyBoolean("FALSE()&&TRUE()&&FALSE()", new Eval().evaluate("FALSE()&&TRUE()&&FALSE()"), false);
        verifyBoolean("FALSE()&&FALSE()&&TRUE()", new Eval().evaluate("FALSE()&&TRUE()&&TRUE()"), false);
        verifyBoolean("FALSE()&&FALSE()&&FALSE()", new Eval().evaluate("FALSE()&&FALSE()&&FALSE()"), false);

        verifyBoolean("TRUE()||TRUE()&&TRUE()", new Eval().evaluate("TRUE()||TRUE()&&TRUE()"), true);
        verifyBoolean("TRUE()||TRUE()&&FALSE()", new Eval().evaluate("TRUE()||TRUE()&&FALSE()"), true);
        verifyBoolean("TRUE()||FALSE()&&TRUE()", new Eval().evaluate("TRUE()||FALSE()&&TRUE()"), true);
        verifyBoolean("TRUE()||FALSE()&&FALSE()", new Eval().evaluate("TRUE()||FALSE()&&FALSE()"), true);
        verifyBoolean("FALSE()||TRUE()&&TRUE()", new Eval().evaluate("FALSE()||TRUE()&&TRUE()"), true);
        verifyBoolean("FALSE()||TRUE()&&FALSE()", new Eval().evaluate("FALSE()||TRUE()&&FALSE()"), false);
        verifyBoolean("FALSE()||FALSE()&&TRUE()", new Eval().evaluate("FALSE()||FALSE()&&TRUE()"), false);
        verifyBoolean("FALSE()||FALSE()&&FALSE()", new Eval().evaluate("FALSE()||FALSE()&&FALSE()"), false);

        verifyBoolean("TRUE()&&TRUE()||TRUE()", new Eval().evaluate("TRUE()&&TRUE()||TRUE()"), true);
        verifyBoolean("TRUE()&&TRUE()||FALSE()", new Eval().evaluate("TRUE()&&TRUE()||FALSE()"), true);
        verifyBoolean("TRUE()&&FALSE()||TRUE()", new Eval().evaluate("TRUE()&&FALSE()||TRUE()"), true);
        verifyBoolean("TRUE()&&FALSE()||FALSE()", new Eval().evaluate("TRUE()&&FALSE()||FALSE()"), false);
        verifyBoolean("FALSE()&&TRUE()||TRUE()", new Eval().evaluate("FALSE()&&TRUE()||TRUE()"), true);
        verifyBoolean("FALSE()&&TRUE()||FALSE()", new Eval().evaluate("FALSE()&&TRUE()||FALSE()"), false);
        verifyBoolean("FALSE()&&FALSE()||TRUE()", new Eval().evaluate("FALSE()&&TRUE()||TRUE()"), true);
        verifyBoolean("FALSE()&&FALSE()||FALSE()", new Eval().evaluate("FALSE()&&FALSE()||FALSE()"), false);

        verifyBoolean("TRUE()||TRUE()||TRUE()", new Eval().evaluate("TRUE()||TRUE()||TRUE()"), true);
        verifyBoolean("TRUE()||TRUE()||FALSE()", new Eval().evaluate("TRUE()||TRUE()||FALSE()"), true);
        verifyBoolean("TRUE()||FALSE()||TRUE()", new Eval().evaluate("TRUE()||FALSE()||TRUE()"), true);
        verifyBoolean("TRUE()||FALSE()||FALSE()", new Eval().evaluate("TRUE()||FALSE()||FALSE()"), true);
        verifyBoolean("FALSE()||TRUE()||TRUE()", new Eval().evaluate("FALSE()||TRUE()||TRUE()"), true);
        verifyBoolean("FALSE()||TRUE()||FALSE()", new Eval().evaluate("FALSE()||TRUE()||FALSE()"), true);
        verifyBoolean("FALSE()||FALSE()||TRUE()", new Eval().evaluate("FALSE()||FALSE()||TRUE()"), true);
        verifyBoolean("FALSE()||FALSE()||FALSE()", new Eval().evaluate("FALSE()||FALSE()||FALSE()"), false);

        verifyValueAndUnit("2^3^5", new Eval().evaluate("2^3^5"), Math.pow(2, Math.pow(3, 5)), 1, Dimensionless.ONE);
    }

    /**
     * Test conditional expressions
     */
    @Test
    public void testConditionalExpressions()
    {
        verifyValueAndUnit("TRUE()?3:(1/0)", new Eval().evaluate("TRUE()?3:(1/0)"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?1/0:3", new Eval().evaluate("FALSE()?1/0:3"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?3:((1/0))", new Eval().evaluate("TRUE()?3:(1/0)"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?((1/0)):3", new Eval().evaluate("FALSE()?1/0:3"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?TRUE()?1:2:3", new Eval().evaluate("TRUE()?TRUE()?1:2:3"), 1, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?FALSE()?1:2:3", new Eval().evaluate("TRUE()?FALSE()?1:2:3"), 2, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?TRUE()?1:2:3", new Eval().evaluate("FALSE()?TRUE()?1:2:3"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?FALSE()?1:2:3", new Eval().evaluate("FALSE()?FALSE()?1:2:3"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?1:TRUE()?2:3", new Eval().evaluate("TRUE()?1:TRUE()?2:3"), 1, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?1:FALSE()?2:3", new Eval().evaluate("TRUE()?1:FALSE()?2:3"), 1, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?1:TRUE()?2:3", new Eval().evaluate("FALSE()?1:TRUE()?2:3"), 2, 0, Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?1:FALSE()?2:3", new Eval().evaluate("FALSE()?1:FALSE()?2:3"), 3, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?(((3))):(((1/0)))", new Eval().evaluate("TRUE()?(((3))):(((1/0)))"), 3, 0,
                Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?(((1/0))):(((5)))", new Eval().evaluate("FALSE()?(((1/0))):(((5)))"), 5, 0,
                Dimensionless.ONE);
        verifyValueAndUnit("FALSE()?1:2+3", new Eval().evaluate("FALSE()?1:2+3"), 5, 0, Dimensionless.ONE);
        verifyValueAndUnit("TRUE()?1:2+3", new Eval().evaluate("TRUE()?1:2+3"), 4, 0, Dimensionless.ONE);
        try
        {
            new Eval().evaluate("TRUE()?1:(");
            fail("Missing closing parentheses should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing closing parenthesis"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("FALSE()?TRUE()");
            fail("Incomplete conditional expression should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            // System.out.println(rte.getMessage());
            assertTrue(rte.getMessage().toLowerCase().contains("nonterminated conditional expression"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()?TRUE()");
            fail("Incomplete conditional expression should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing \':\' of conditional expression"),
                    "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("TRUE()?TRUE()?");
            fail("Incomplete conditional expression should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("missing operand"), "Message describes the problem");
        }

        try
        {
            new Eval().evaluate("123?4:5");
            fail("Conditional expression depending on non-logical should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("condition does not evaluate to a logical value"),
                    "Message describes the problem");
        }

        verifyBoolean("Binding strength of else part is minimum", new Eval().evaluate("12>16?3:FALSE()||TRUE()"), true);
        verifyBoolean("Binding strength of else part is minimum", new Eval().evaluate("12>16?3:5>4"), true);
    }

    /**
     * Test the mechanism for retrieving the values from a value store by name and the handling of absolute operands.
     */
    @Test
    public void testNamedVariablesAndHandlingOfAbsoluteOperands()
    {
        try
        {
            new Eval().evaluate("abc");
            fail("Attempt to retrieve value with no value store should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot resolve variable "), "Message describes the problem");
        }

        Map<String, Object> map = new HashMap<>();
        ValueStore valueStore = new ValueStore(map);

        try
        {
            new Eval().setRetrieveValue(valueStore).evaluate("abc");
            fail("Attempt to retrieve value with empty value store should throw a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("cannot resolve variable "), "Message describes the problem");
        }
    }

    /**
     * Test the user-provided unit parser hook.
     */
    @Test
    public void testUserParser()
    {
        Eval eval = new Eval();
        Object resultObject = eval.evaluate("123[ms]");
        assertTrue(resultObject instanceof Quantity, "result is a Quantity");
        Quantity<?> resultds = (Quantity<?>) resultObject;
        assertEquals(123, resultds.si, 0, "value is 123");
        SIUnit siDimensions = resultds.siUnit();
        assertTrue(siDimensions.equals(new SIUnit(new int[] {0, 0, 0, 1, 1, 0, 0, 0, 0})), "SI dimensions match");
        // Create and install a user parser for milli seconds
        QuantityParser unitParser = new QuantityParser()
        {
            @Override
            public Quantity<?> parseQuantity(final double value, final String unit)
            {
                if (unit.equals("ms"))
                {
                    return new Duration(value, Duration.Unit.ms);
                }
                // Anything else is not handled by this UnitParser
                return null;
            }
        };
        eval.setUnitParser(unitParser);
        Object newResultObject = eval.evaluate("123[ms]");
        assertEquals(0.123, ((Quantity<?>) newResultObject).si, 0.0000001, "value is 0.123");
        assertEquals(Duration.Unit.ms, ((Quantity<?>) newResultObject).getDisplayUnit(), "unit is ms");
        // Parse something that is not handled by the user unit parser
        verifyValueAndUnit("Parse something that is not handled by the user unit parser", eval.evaluate("123[m/s]"), 123, 0.0,
                Speed.ZERO);
        // Uninstall the user parser
        eval.setUnitParser(null);
        Object oldResult = eval.evaluate("123[ms]");
        assertEquals(resultObject, oldResult, "user unit parser is no longer active");
    }

    /**
     * Test the evaluateAs methods
     */
    @Test
    public void testEvaluateAsMethods()
    {
        Object result = new Eval().evaluateAsDouble("123");
        assertTrue(result instanceof Double, "result is a Double");
        assertEquals(123, ((Double) result), 0, "value is 123");
        try
        {
            new Eval().evaluateAsBoolean("123");
            fail("evaluateAsBoolean for a non-boolean result should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("can not be cast to "), "Message describes the problem");
        }

        result = new Eval().evaluateAsBoolean("2>3");
        assertTrue(result instanceof Boolean, "result is a Boolean");
        assertFalse((Boolean) result, "result is false");
        try
        {
            new Eval().evaluateAsDouble("2>3");
            fail("evaluateAsDouble for a boolean result should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("can not be cast to "), "Message describes the problem");
        }

    }

    /**
     * Test the use of user-defined functions.
     */
    @Test
    public void testUserFunctions()
    {
        Map<String, Function> map = new HashMap<>();
        Eval eval = new Eval();
        try
        {
            eval.evaluate("ceil(5.1)");
            fail("Calling an undefined function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function "), "Message describes the problem");
        }

        // Install the (still empty) map
        eval.setUserDefinedFunctions(map);
        try
        {
            eval.evaluate("ceil(5.1)");
            fail("Calling an undefined function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function "), "Message describes the problem");
        }

        Function ceil = new Function()
        {

            @Override
            public String getId()
            {
                return "ceil";
            }

            @Override
            public MetaData getMetaData()
            {
                return new MetaData("ceil", "returns the ceil of the argument (which must be Dimensionless)",
                        new ObjectDescriptor("argument", "argument", Dimensionless.class));
            }

            @Override
            public Object function(final Object[] arguments) throws RuntimeException
            {
                if (arguments.length != 1)
                {
                    throw new RuntimeException("ceil requires one argument (got " + arguments.length + ")");
                }
                if (!(arguments[0] instanceof Quantity))
                {
                    throw new RuntimeException("argument of ceil should be a Quantity");
                }
                return new Dimensionless(Math.ceil(((Quantity<?>) (arguments[0])).si), Unitless.BASE);
            }
        };
        map.put(ceil.getId(), ceil);
        // The map is copied when installed, so it must be installed again after adding the function
        eval.setUserDefinedFunctions(map);
        verifyValueAndUnit("ceil should now work", eval.evaluateExpression("ceil(5.1)"), 6, 0.0000001, Dimensionless.ONE);
        // Remove all user defined functions
        eval.setUserDefinedFunctions(null);
        try
        {
            eval.evaluate("ceil(5.1)");
            fail("Calling an undefined function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function "), "Message describes the problem");
        }

        // Remove our function and reinstall the map
        map.remove("ceil");
        eval.setUserDefinedFunctions(map);
        try
        {
            eval.evaluate("ceil(5.1)");
            fail("Calling an undefined function should have thrown a RuntimeException");
        }
        catch (RuntimeException rte)
        {
            assertTrue(rte.getMessage().toLowerCase().contains("unknown function "), "Message describes the problem");
        }

    }

    /**
     * Test the builtinFunctions method.
     */
    @Test
    public void testBuiltinFunction()
    {
        Eval eval = new Eval();
        Collection<Function> collection = eval.builtInFunctions();
        assertNotNull(collection, "result may not be null");
        boolean foundAtan2 = false;
        boolean foundAcos = false;
        boolean foundAvogadro = false;
        for (Function f : collection)
        {
            switch (f.getId())
            {
                case "atan2":
                    foundAtan2 = true;
                    break;

                case "acos":
                    foundAcos = true;
                    break;

                case "AVOGADRO":
                    foundAvogadro = true;
                    break;
            }
        }
        assertTrue(foundAtan2, "Found atan2");
        assertTrue(foundAcos, "Found acos");
        assertTrue(foundAvogadro, "Found Avogadro");
    }

    /**
     * Value store for testing the value retrieval system.
     */
    class ValueStore implements RetrieveValue
    {
        /** The mapping from name to value. */
        private final Map<String, Object> values;

        /**
         * Create a new ValueStore.
         * @param map map that translates names to value (not deep-copied; therefore, if this map is changed at a later time
         *            that will affect subsequent lookup results).
         */
        ValueStore(final Map<String, Object> map)
        {
            this.values = map;
        }

        @Override
        public Object lookup(final String name)
        {
            return this.values.get(name);
        }

    }

    /**
     * Verify the class, value and unit of a Quantity value.
     * @param description description of the test
     * @param object the Quantity
     * @param expectedValue the expected SI value
     * @param tolerance the maximum error of the SI value
     * @param expectedQuantity the expected quantity
     */
    private void verifyValueAndUnit(final String description, final Object object, final double expectedValue,
            final double tolerance, final Quantity<?> expectedQuantity)
    {
        if (!(object instanceof Quantity))
        {
            System.out.println("object: " + object.getClass().getCanonicalName());
        }
        assertTrue(object instanceof Quantity, description);
        Quantity<?> ds = (Quantity<?>) object;
        assertEquals(ds.si, expectedValue, tolerance, description);
        assertEquals(ds.siUnit(), expectedQuantity.siUnit(), description);
    }

    /**
     * Verify the class and value of a Boolean value
     * @param description description of the test
     * @param object the Boolean
     * @param expectedValue the expected value
     */
    private void verifyBoolean(final String description, final Object object, final boolean expectedValue)
    {
        assertTrue(object instanceof Boolean, description);
        Boolean bv = (Boolean) object;
        assertEquals(bv, expectedValue, description);
    }
}
