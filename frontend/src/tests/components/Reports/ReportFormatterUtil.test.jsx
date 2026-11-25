import { formatCurrency } from "main/components/Reports/ReportFormatterUtil";

describe("formatCurrency tests", () => {
  test("formats amounts in US dollars", () => {
    expect(formatCurrency(1234.56)).toBe("$1,234.56");
    expect(formatCurrency(0)).toBe("$0.00");
    expect(formatCurrency(-12.5)).toBe("-$12.50");
  });
});
