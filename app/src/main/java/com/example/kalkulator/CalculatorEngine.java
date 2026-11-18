package com.example.kalkulator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Pure Java calculation engine for CALQ.
 * Uses BigDecimal to prevent floating-point rounding artifacts (e.g., 0.1 + 0.2).
 */
public final class CalculatorEngine {

    private static final MathContext MATH_CONTEXT = new MathContext(15, RoundingMode.HALF_UP);
    private static final int DISPLAY_SCALE = 10;

    private CalculatorEngine() {
    }

    public static class EvaluationResult {
        public final boolean success;
        public final String formattedResult;
        public final String errorMessage;

        private EvaluationResult(boolean success, String formattedResult, String errorMessage) {
            this.success = success;
            this.formattedResult = formattedResult;
            this.errorMessage = errorMessage;
        }

        public static EvaluationResult ok(String formattedResult) {
            return new EvaluationResult(true, formattedResult, null);
        }

        public static EvaluationResult error(String errorMessage) {
            return new EvaluationResult(false, "", errorMessage);
        }
    }

    public static class DiscountResult {
        public final BigDecimal savings;
        public final BigDecimal finalPrice;

        public DiscountResult(BigDecimal savings, BigDecimal finalPrice) {
            this.savings = savings;
            this.finalPrice = finalPrice;
        }
    }

    public static EvaluationResult evaluate(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return EvaluationResult.error("Ekspresi kosong");
        }

        String expr = normalizeExpression(rawExpression);

        try {
            List<String> tokens = tokenize(expr);
            if (tokens.isEmpty()) {
                return EvaluationResult.error("Ekspresi tidak valid");
            }
            List<String> rpn = toRpn(tokens);
            BigDecimal value = evalRpn(rpn);
            return EvaluationResult.ok(formatNumber(value));
        } catch (ArithmeticException e) {
            String msg = e.getMessage() != null && e.getMessage().contains("nol")
                    ? "Tidak bisa dibagi 0"
                    : "Kesalahan hitung";
            return EvaluationResult.error(msg);
        } catch (Exception e) {
            return EvaluationResult.error("Ekspresi tidak valid");
        }
    }

    private static String normalizeExpression(String rawExpression) {
        String expr = rawExpression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replaceAll("\\s+", "");

        if (expr.contains(",")) {
            // Comma is the decimal separator (Indonesian standard)
            // Remove dots (thousands separators) and replace comma with dot
            expr = expr.replace(".", "").replace(",", ".");
        } else {
            // Check if dot was used as thousand separator (e.g. 1.000, 1.000.000)
            expr = expr.replaceAll("(?<=\\d)\\.(?=\\d{3}(?:\\.\\d{3})*(?:[^0-9]|$))", "");
        }
        return expr;
    }

    public static String formatNumber(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        BigDecimal rounded = value.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        String plain = rounded.toPlainString().replace(".", ",");
        return formatNumberString(plain);
    }

    public static String formatNumberString(String numberStr) {
        if (numberStr == null || numberStr.isEmpty()) {
            return "";
        }
        boolean isNegative = numberStr.startsWith("-") || numberStr.startsWith("−");
        String clean = isNegative ? numberStr.substring(1) : numberStr;

        String intPart;
        String decPart = "";
        int sepIdx = clean.indexOf(',');
        if (sepIdx == -1) {
            sepIdx = clean.indexOf('.');
        }

        if (sepIdx != -1) {
            intPart = clean.substring(0, sepIdx);
            decPart = "," + clean.substring(sepIdx + 1);
        } else {
            intPart = clean;
        }

        if (intPart.isEmpty()) {
            intPart = "0";
        }

        StringBuilder grouped = new StringBuilder();
        int len = intPart.length();
        for (int i = 0; i < len; i++) {
            grouped.append(intPart.charAt(i));
            int remaining = len - 1 - i;
            if (remaining > 0 && remaining % 3 == 0) {
                grouped.append('.');
            }
        }

        return (isNegative ? "-" : "") + grouped + decPart;
    }

    public static String formatExpressionForDisplay(String rawExpr) {
        if (rawExpr == null || rawExpr.trim().isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        StringBuilder numBuffer = new StringBuilder();
        int n = rawExpr.length();

        for (int i = 0; i < n; i++) {
            char c = rawExpr.charAt(i);

            if (Character.isDigit(c) || c == ',' || c == '.') {
                numBuffer.append(c);
            } else {
                if (numBuffer.length() > 0) {
                    formatted.append(formatNumberString(numBuffer.toString()));
                    numBuffer.setLength(0);
                }

                if (c == '+' || c == '×' || c == '*' || c == '÷' || c == '/') {
                    char displayOp = (c == '*' || c == '×') ? '×' : (c == '/' || c == '÷') ? '÷' : '+';
                    formatted.append(" ").append(displayOp).append(" ");
                } else if (c == '-' || c == '−') {
                    boolean isUnary = formatted.length() == 0
                            || formatted.charAt(formatted.length() - 1) == ' '
                            || formatted.charAt(formatted.length() - 1) == '(';
                    if (isUnary) {
                        numBuffer.append('-');
                    } else {
                        formatted.append(" − ");
                    }
                } else {
                    formatted.append(c);
                }
            }
        }

        if (numBuffer.length() > 0) {
            formatted.append(formatNumberString(numBuffer.toString()));
        }

        return formatted.toString().replaceAll(" +", " ").trim();
    }

    private static List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder num = new StringBuilder();
                int dotCount = 0;
                while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    char ch = expr.charAt(i);
                    if (ch == '.') {
                        dotCount++;
                        if (dotCount > 1) {
                            throw new IllegalArgumentException("Format angka tidak valid");
                        }
                    }
                    num.append(ch);
                    i++;
                }
                String numStr = num.toString();
                if (".".equals(numStr)) {
                    throw new IllegalArgumentException("Titik desimal tanpa angka");
                }
                if (numStr.startsWith(".")) {
                    numStr = "0" + numStr;
                }
                if (numStr.endsWith(".")) {
                    numStr = numStr.substring(0, numStr.length() - 1);
                }
                tokens.add(numStr);
                continue;
            }

            if (c == '-' && isUnaryPosition(tokens)) {
                if (i + 1 < n && (Character.isDigit(expr.charAt(i + 1)) || expr.charAt(i + 1) == '.')) {
                    i++;
                    StringBuilder num = new StringBuilder("-");
                    int dotCount = 0;
                    while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                        char ch = expr.charAt(i);
                        if (ch == '.') {
                            dotCount++;
                            if (dotCount > 1) {
                                throw new IllegalArgumentException("Format angka tidak valid");
                            }
                        }
                        num.append(ch);
                        i++;
                    }
                    String numStr = num.toString();
                    if (numStr.startsWith("-.")) {
                        numStr = "-0." + numStr.substring(2);
                    }
                    if (numStr.endsWith(".")) {
                        numStr = numStr.substring(0, numStr.length() - 1);
                    }
                    tokens.add(numStr);
                    continue;
                } else {
                    tokens.add("0");
                    tokens.add("-");
                    i++;
                    continue;
                }
            }

            if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '(' || c == ')') {
                if (c == '(' && !tokens.isEmpty()) {
                    String prev = tokens.get(tokens.size() - 1);
                    if (isNumber(prev) || ")".equals(prev) || "%".equals(prev)) {
                        tokens.add("*");
                    }
                }
                tokens.add(String.valueOf(c));
                i++;
            } else {
                throw new IllegalArgumentException("Karakter tidak dikenali: " + c);
            }
        }
        return tokens;
    }

    private static boolean isUnaryPosition(List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String last = tokens.get(tokens.size() - 1);
        return "+".equals(last) || "-".equals(last) || "*".equals(last)
                || "/".equals(last) || "(".equals(last);
    }

    private static List<String> toRpn(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> ops = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if ("%".equals(token)) {
                output.add("%");
            } else if ("(".equals(token)) {
                ops.push(token);
            } else if (")".equals(token)) {
                while (!ops.isEmpty() && !"(".equals(ops.peek())) {
                    output.add(ops.pop());
                }
                if (ops.isEmpty() || !"(".equals(ops.peek())) {
                    throw new IllegalArgumentException("Kurung tidak seimbang");
                }
                ops.pop();
            } else if (isBinaryOperator(token)) {
                while (!ops.isEmpty() && isBinaryOperator(ops.peek())
                        && precedence(ops.peek()) >= precedence(token)) {
                    output.add(ops.pop());
                }
                ops.push(token);
            } else {
                throw new IllegalArgumentException("Token tidak valid: " + token);
            }
        }

        while (!ops.isEmpty()) {
            String op = ops.pop();
            if ("(".equals(op) || ")".equals(op)) {
                throw new IllegalArgumentException("Kurung tidak seimbang");
            }
            output.add(op);
        }

        return output;
    }

    private static BigDecimal evalRpn(List<String> rpn) {
        Stack<BigDecimal> stack = new Stack<>();
        BigDecimal hundred = BigDecimal.valueOf(100);

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(new BigDecimal(token));
            } else if ("%".equals(token)) {
                if (stack.isEmpty()) {
                    throw new IllegalArgumentException("Persen tidak valid");
                }
                BigDecimal val = stack.pop();
                stack.push(val.divide(hundred, MATH_CONTEXT));
            } else if (isBinaryOperator(token)) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Operand kurang");
                }
                BigDecimal b = stack.pop();
                BigDecimal a = stack.pop();
                switch (token) {
                    case "+":
                        stack.push(a.add(b, MATH_CONTEXT));
                        break;
                    case "-":
                        stack.push(a.subtract(b, MATH_CONTEXT));
                        break;
                    case "*":
                        stack.push(a.multiply(b, MATH_CONTEXT));
                        break;
                    case "/":
                        if (b.compareTo(BigDecimal.ZERO) == 0) {
                            throw new ArithmeticException("Pembagian dengan nol");
                        }
                        stack.push(a.divide(b, MATH_CONTEXT));
                        break;
                    default:
                        throw new IllegalArgumentException("Operator tidak dikenal");
                }
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Ekspresi tidak lengkap");
        }
        return stack.pop();
    }

    private static boolean isNumber(String token) {
        try {
            new BigDecimal(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBinaryOperator(String token) {
        return "+".equals(token) || "-".equals(token) || "*".equals(token) || "/".equals(token);
    }

    private static int precedence(String op) {
        if ("+".equals(op) || "-".equals(op)) {
            return 1;
        }
        if ("*".equals(op) || "/".equals(op)) {
            return 2;
        }
        return 0;
    }

    public static BigDecimal calculatePercentage(BigDecimal percent, BigDecimal value) {
        return percent.divide(BigDecimal.valueOf(100), MATH_CONTEXT).multiply(value, MATH_CONTEXT);
    }

    public static DiscountResult calculateDiscount(BigDecimal originalPrice, BigDecimal discountPercent) {
        BigDecimal savings = calculatePercentage(discountPercent, originalPrice);
        BigDecimal finalPrice = originalPrice.subtract(savings, MATH_CONTEXT);
        return new DiscountResult(savings, finalPrice);
    }

    public static BigDecimal celsiusToFahrenheit(BigDecimal celsius) {
        return celsius.multiply(BigDecimal.valueOf(9), MATH_CONTEXT)
                .divide(BigDecimal.valueOf(5), MATH_CONTEXT)
                .add(BigDecimal.valueOf(32), MATH_CONTEXT);
    }

    public static BigDecimal fahrenheitToCelsius(BigDecimal fahrenheit) {
        return fahrenheit.subtract(BigDecimal.valueOf(32), MATH_CONTEXT)
                .multiply(BigDecimal.valueOf(5), MATH_CONTEXT)
                .divide(BigDecimal.valueOf(9), MATH_CONTEXT);
    }
}
