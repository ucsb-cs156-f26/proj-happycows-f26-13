import { fireEvent, render, screen, within } from "@testing-library/react";
import { vi } from "vitest";
import HerdList from "main/components/Game/HerdList";
import herdFixtures from "fixtures/herdFixtures";

describe("HerdList", () => {
  test("shows each herd's count and health and sums all cows", () => {
    render(<HerdList herds={herdFixtures.severalHerds} />);
    const table = screen.getByRole("table", { name: "Your herds" });
    const rows = within(table).getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(
      within(rows[0])
        .getAllByRole("columnheader")
        .map((cell) => cell.textContent),
    ).toEqual(["Herd", "Cows", "Health"]);
    expect(within(rows[1]).getByRole("rowheader")).toHaveTextContent("Herd #1");
    expect(
      within(rows[1])
        .getAllByRole("cell")
        .map((cell) => cell.textContent),
    ).toEqual(["3", "100.00%"]);
    expect(within(rows[2]).getByRole("rowheader")).toHaveTextContent("Herd #2");
    expect(
      within(rows[2])
        .getAllByRole("cell")
        .map((cell) => cell.textContent),
    ).toEqual(["7", "62.34%"]);
    expect(screen.getByText("Total cows in herds: 10")).toBeInTheDocument();
    // herds are numbered per farm (herdNumber), not by database id
    expect(screen.queryByText("Herd #11")).not.toBeInTheDocument();
    expect(screen.getByText("Your Herds")).toBeInTheDocument();
    expect(
      screen.queryByText("You have no herds yet."),
    ).not.toBeInTheDocument();
  });

  test("shows a single herd with zero health", () => {
    render(
      <HerdList herds={[{ id: 4, herdNumber: 1, numCows: 2, health: 0 }]} />,
    );
    expect(screen.getAllByRole("row")).toHaveLength(2);
    expect(screen.getByText("0.00%")).toBeInTheDocument();
    expect(screen.getByText("Total cows in herds: 2")).toBeInTheDocument();
  });

  test.each([undefined, []])("shows an empty state for %j", (herds) => {
    render(<HerdList herds={herds} />);
    expect(screen.getByText("You have no herds yet.")).toBeInTheDocument();
    expect(screen.getByText("Total cows in herds: 0")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  test("shows loading instead of misleading empty data", () => {
    render(<HerdList isLoading />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading herds...");
    expect(
      screen.queryByText("You have no herds yet."),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  test("shows an error instead of stale data and allows retrying", () => {
    const onRetry = vi.fn();
    render(
      <HerdList herds={herdFixtures.severalHerds} isError onRetry={onRetry} />,
    );
    expect(screen.getByRole("alert")).toHaveTextContent(
      "Unable to load your herds.",
    );
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
    expect(
      screen.queryByText("You have no herds yet."),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(onRetry).toHaveBeenCalledExactlyOnceWith();
  });
});
