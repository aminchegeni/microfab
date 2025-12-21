package ir.co.isc.spbp.blockchain.fabino.unit;

import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.*;

/**
 * Resolves template variables of the form {@code {{variableName}}} within strings.
 *
 * <p>This utility performs a lightweight, non-recursive placeholder substitution
 * using a predefined regular expression. Each placeholder is replaced with the
 * corresponding value from the provided variables map.</p>
 *
 * <p>If a variable is not found in the map, the placeholder is left unchanged.
 * This design avoids hard failures and allows partial resolution.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * template:  "Hello {{ user }}, block {{height}}"
 * variables: { "user": "Alice", "height": 42 }
 * result:    "Hello Alice, block 42"
 * }</pre>
 *
 * <p>This class is intentionally simple and deterministic, making it suitable
 * for test infrastructure, configuration expansion, and mock environments.</p>
 */
public class TemplateVariableResolver {

    /**
     * Regular expression used to detect variable placeholders.
     *
     * <p>Supported variable names follow Java identifier rules and are enclosed
     * in double curly braces, for example: {@code {{name}}}, {@code {{_value}}}.</p>
     */
    private static final Pattern VARIABLE_PLACEHOLDER = Pattern.compile(
            "\\{\\{\\s*([a-zA-Z_$][a-zA-Z\\d_$]*)\\s*}}"
    );

    /**
     * Resolves template variables for an array of template strings.
     *
     * <p>Each template is processed independently using the same variables map.
     * The resulting array preserves the original order.</p>
     *
     * @param templates the array of template strings to resolve (must not be {@code null})
     * @param variables the variable map used for substitution (must not be {@code null})
     * @return a new array containing resolved template strings
     * @throws NullPointerException if {@code templates} is {@code null}
     */
    public String[] resolve(String[] templates, Map<String, Object> variables) {
        requireNonNull(templates);
        String[] result = new String[templates.length];
        for (int i = 0; i < templates.length; i++) {
            result[i] = resolve(templates[i], variables);
        }
        return result;
    }

    /**
     * Resolves template variables within a single template string.
     *
     * <p>Each occurrence of {@code {{variableName}}} is replaced with the
     * {@link Object#toString()} value of the corresponding entry in the
     * variables map.</p>
     *
     * <p>If a variable is missing or mapped to {@code null}, the placeholder
     * remains unchanged in the output.</p>
     *
     * <p>This method does not perform recursive resolution or expression
     * evaluation. Substitution is purely textual.</p>
     *
     * @param template the template string to resolve (must not be {@code null})
     * @param variables the variable map used for substitution (must not be {@code null})
     * @return the resolved template string
     * @throws NullPointerException if {@code template} or {@code variables} is {@code null}
     */
    public String resolve(String template, Map<String, Object> variables) {
        requireNonNull(template);
        requireNonNull(variables);
        return VARIABLE_PLACEHOLDER.matcher(template)
                .replaceAll(match -> {
                    String varKey = match.group(1);
                    Object varVal = variables.get(varKey);

                    // If the variable is missing, keep the original placeholder
                    return nonNull(varVal) ? varVal.toString() : match.group(0);
                });
    }
}
