package com.pgrdaw.tagfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgrdaw.tagfolio.model.Image;
import com.pgrdaw.tagfolio.model.Tag;
import com.pgrdaw.tagfolio.service.util.FilterFieldService;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service for evaluating filter expressions.
 *
 * @author Pablo Gimeno Ramallo &lt;pgrdaw@gmail.com&gt;
 * @since 2026-01-01
 */
@Service
public class FilterExpressionEvaluator {

    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();

    static {
        PRECEDENCE.put("OR", 1);
        PRECEDENCE.put("AND", 2);
        PRECEDENCE.put("NOT", 3);
    }

    private static class DateRange {
        private final LocalDateTime start;
        private final LocalDateTime end;

        DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }

    private final FilterFieldService filterFieldService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new FilterExpressionEvaluator.
     *
     * @param filterFieldService The filter field service.
     * @param objectMapper       The object mapper for JSON processing.
     */
    public FilterExpressionEvaluator(FilterFieldService filterFieldService, ObjectMapper objectMapper) {
        this.filterFieldService = filterFieldService;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates a filter expression against a list of images.
     *
     * @param allImages  The list of images to evaluate against.
     * @param expression The filter expression.
     * @return A list of IDs of the images that match the expression.
     */
    public List<Long> evaluate(List<Image> allImages, List<Map<String, String>> expression) {
        if (expression == null || expression.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, String>> consolidatedExpression = consolidateExpression(expression);

        List<Map<String, String>> rpn = convertToRPN(consolidatedExpression);
        if (rpn == null) {
            return Collections.emptyList();
        }

        return allImages.stream()
                .filter(image -> evaluateRPN(rpn, image))
                .map(Image::getId)
                .collect(Collectors.toList());
    }

    private List<Map<String, String>> consolidateExpression(List<Map<String, String>> expression) {
        List<Map<String, String>> result = new ArrayList<>();
        List<Map<String, String>> temp = new ArrayList<>(expression);

        for (int i = 0; i < temp.size() - 1; i++) {
            Map<String, String> current = temp.get(i);
            Map<String, String> next = temp.get(i + 1);

            if ("comparator".equals(current.get("type")) && "comparator".equals(next.get("type"))) {
                String combinedValue = current.get("value") + next.get("value");
                if (Arrays.asList(">=", "<=").contains(combinedValue)) {
                    Map<String, String> newComparator = new HashMap<>();
                    newComparator.put("type", "comparator");
                    newComparator.put("value", combinedValue);
                    temp.set(i, newComparator);
                    temp.remove(i + 1);
                    i--;
                }
            }
        }

        for (int i = 0; i < temp.size(); i++) {
            Map<String, String> item = temp.get(i);
            if ("comparator-field".equals(item.get("type")) && i + 2 < temp.size()) {
                Map<String, String> next1 = temp.get(i + 1);
                Map<String, String> next2 = temp.get(i + 2);

                if ("comparator".equals(next1.get("type")) && "value".equals(next2.get("type"))) {
                    String fieldRaw = item.get("value");
                    String op = next1.get("value");
                    String rawValue = next2.get("value");

                    String fieldCanonical = filterFieldService.getAllowedFields().stream()
                            .filter(f -> f.equalsIgnoreCase(fieldRaw))
                            .findFirst()
                            .orElse(null);

                    if (fieldCanonical != null) {
                        Map<String, String> newField = new HashMap<>();
                        newField.put("type", "field");
                        newField.put("field", fieldCanonical);
                        newField.put("op", op);
                        newField.put("rawValue", rawValue);
                        newField.put("value", String.format("%s%s%s", fieldCanonical, op, rawValue));
                        result.add(newField);
                        i += 2;
                        continue;
                    }
                }
            }
            result.add(item);
        }
        return result;
    }

    private boolean evaluateRPN(List<Map<String, String>> rpn, Image image) {
        Set<String> imageTags = image.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
        Deque<Boolean> stack = new ArrayDeque<>();

        for (Map<String, String> item : rpn) {
            String type = item.get("type");
            String value = item.get("value");

            if ("tag".equals(type)) {
                boolean matches = imageTags.contains(value);
                stack.push(matches);
            } else if ("field".equals(type)) {
                boolean matches = evaluateFieldPredicate(item, image);
                stack.push(matches);
            } else if ("operator".equals(type)) {
                if ("NOT".equals(value)) {
                    if (stack.isEmpty()) {
                        return false;
                    }
                    boolean op = stack.pop();
                    stack.push(!op);
                } else {
                    if (stack.size() < 2) {
                        return false;
                    }
                    boolean op2 = stack.pop();
                    boolean op1 = stack.pop();
                    if ("AND".equals(value)) {
                        stack.push(op1 && op2);
                    } else if ("OR".equals(value)) {
                        stack.push(op1 || op2);
                    }
                }
            }
        }

        return stack.size() == 1 && stack.pop();
    }

    private List<Map<String, String>> convertToRPN(List<Map<String, String>> infix) {
        Deque<Map<String, String>> outputQueue = new ArrayDeque<>();
        Deque<Map<String, String>> operatorStack = new ArrayDeque<>();

        for (Map<String, String> item : infix) {
            String type = item.get("type");
            String value = item.get("value");

            switch (type) {
                case "tag":
                case "field":
                    outputQueue.add(item);
                    break;
                case "operator":
                    while (!operatorStack.isEmpty() && "operator".equals(operatorStack.peek().get("type")) &&
                            (PRECEDENCE.get(operatorStack.peek().get("value")) > PRECEDENCE.get(value) ||
                                    (PRECEDENCE.get(operatorStack.peek().get("value")).equals(PRECEDENCE.get(value)) && !"NOT".equals(value)))) {
                        outputQueue.add(operatorStack.pop());
                    }
                    operatorStack.push(item);
                    break;
                case "parenthesis":
                    if ("(".equals(value)) {
                        operatorStack.push(item);
                    } else {
                        while (!operatorStack.isEmpty() && !"(".equals(operatorStack.peek().get("value"))) {
                            outputQueue.add(operatorStack.pop());
                        }
                        if (operatorStack.isEmpty()) {
                            return null;
                        }
                        operatorStack.pop();
                    }
                    break;
            }
        }

        while (!operatorStack.isEmpty()) {
            Map<String, String> op = operatorStack.pop();
            if ("(".equals(op.get("value"))) {
                return null;
            }
            outputQueue.add(op);
        }

        return new ArrayList<>(outputQueue);
    }

    private boolean evaluateFieldPredicate(Map<String, String> item, Image image) {
        String field = safeLower(item.get("field"));
        String op = item.getOrDefault("op", "=");
        String rawValue = item.get("rawValue");
        if (rawValue == null) rawValue = item.get("value");

        if (field == null || op == null || rawValue == null) {
            return false;
        }

        switch (field) {
            case "created":
            case "modified":
            case "imported":
                LocalDateTime fieldValue = switch (field) {
                    case "created" -> image.getCreatedAt();
                    case "modified" -> image.getModifiedAt();
                    case "imported" -> image.getImportedAt();
                    default -> null;
                };

                if (fieldValue == null) return false;

                DateRange range = parseDateRange(rawValue);
                if (range == null) return false;

                return switch (op) {
                    case "=" -> !fieldValue.isBefore(range.start) && !fieldValue.isAfter(range.end);
                    case "<" -> fieldValue.isBefore(range.start);
                    case "<=" -> !fieldValue.isAfter(range.end);
                    case ">" -> fieldValue.isAfter(range.end);
                    case ">=" -> !fieldValue.isBefore(range.start);
                    default -> false;
                };
            case "rating":
                Integer rating = image.getRating();
                if (rating == null) return false;
                Integer value = parseInteger(rawValue);
                if (value == null) return false;
                return compareNumbers(rating, value, op);
            default:
                return false;
        }
    }

    private Integer parseInteger(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean compareNumbers(int a, int b, String op) {
        return switch (op) {
            case "<" -> a < b;
            case ">" -> a > b;
            case "=" -> a == b;
            case "<=" -> a <= b;
            case ">=" -> a >= b;
            default -> false;
        };
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = Arrays.asList(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-MM"),
            DateTimeFormatter.ofPattern("yyyy")
    );

    private LocalDateTime parseDateString(String dateString) {
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
            }
        }
        throw new DateTimeParseException("Unable to parse date string: " + dateString, dateString, 0);
    }

    private DateRange parseDateRange(String s) {
        String str = s.trim();

        try {
            if (str.matches("^\\d{4}$")) {
                int year = Integer.parseInt(str);
                LocalDateTime start = LocalDateTime.of(year, 1, 1, 0, 0, 0);
                LocalDateTime end = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999999999);
                return new DateRange(start, end);
            } else if (str.matches("^\\d{4}-\\d{2}$")) {
                String[] parts = str.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                LocalDateTime start = LocalDateTime.of(year, month, 1, 0, 0, 0);
                LocalDateTime end = start.withDayOfMonth(start.toLocalDate().lengthOfMonth())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
                return new DateRange(start, end);
            } else if (str.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                LocalDate date = LocalDate.parse(str);
                LocalDateTime start = date.atStartOfDay();
                LocalDateTime end = date.atTime(23, 59, 59, 999999999);
                return new DateRange(start, end);
            } else {
                LocalDateTime dateTime = parseDateString(str.replace(' ', 'T'));
                return new DateRange(dateTime, dateTime);
            }
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
    }

    private String safeLower(String s) {
        return s == null ? null : s.toLowerCase();
    }

    /**
     * Parses a JSON string into a filter expression.
     *
     * @param expressionJson The JSON string representing the filter expression.
     * @return A list of maps representing the filter expression.
     * @throws JsonProcessingException if an error occurs during JSON processing.
     */
    public List<Map<String, String>> parseExpressionJson(String expressionJson) throws JsonProcessingException {
        return objectMapper.readValue(expressionJson, new TypeReference<>() {
        });
    }
}
