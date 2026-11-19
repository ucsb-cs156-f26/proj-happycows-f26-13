import { fireEvent, render, screen, within } from "@testing-library/react";
import { vi } from "vitest";
import SellHerdsModal from "main/components/Game/SellHerdsModal";
import herdFixtures from "fixtures/herdFixtures";

const mockToastError = vi.fn();
vi.mock("react-toastify", async () => {
  const originalModule = await vi.importActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: {
      ...originalModule.toast,
      error: (message) => mockToastError(message),
    },
  };
});

// herd 11: 3 cows at 100% health; herd 12: 7 cows at 62.345% health
const herds = herdFixtures.severalHerds;
const game = { cowPrice: 100, sellingFee: 2 };

describe("SellHerdsModal", () => {
  const mockOnClose = vi.fn();
  const mockOnSell = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderModal = (props = {}) =>
    render(
      <SellHerdsModal
        isOpen={true}
        onClose={mockOnClose}
        herds={herds}
        game={game}
        onSell={mockOnSell}
        {...props}
      />,
    );

  const typeCount = (herdId, value) =>
    fireEvent.change(screen.getByTestId(`sell-herds-modal-input-${herdId}`), {
      target: { value },
    });

  const total = () => screen.getByTestId("sell-herds-modal-total");

  test("renders nothing when closed", () => {
    renderModal({ isOpen: false });
    expect(screen.queryByTestId("sell-herds-modal")).not.toBeInTheDocument();
  });

  test("lists each herd with its cows, health, and an empty input", () => {
    renderModal();
    expect(screen.getByText("Sell cows")).toBeInTheDocument();
    expect(
      screen.getByText("Choose how many cows to sell from each herd."),
    ).toBeInTheDocument();
    const table = screen.getByRole("table", { name: "Sell from your herds" });
    const rows = within(table).getAllByRole("row");
    expect(rows).toHaveLength(3);
    expect(
      within(rows[0])
        .getAllByRole("columnheader")
        .map((cell) => cell.textContent),
    ).toEqual(["Herd", "Cows", "Health", "Sell"]);
    expect(within(rows[1]).getByRole("rowheader")).toHaveTextContent(
      "Herd #11",
    );
    expect(within(rows[1]).getByText("3")).toBeInTheDocument();
    expect(within(rows[1]).getByText("100.00%")).toBeInTheDocument();
    expect(within(rows[2]).getByRole("rowheader")).toHaveTextContent(
      "Herd #12",
    );
    expect(within(rows[2]).getByText("62.34%")).toBeInTheDocument();

    const input = screen.getByLabelText("Cows to sell from Herd #12");
    expect(input).toHaveValue(null);
    expect(input).toHaveAttribute("type", "number");
    expect(input).toHaveAttribute("min", "0");
    expect(input).toHaveAttribute("max", "7");
    expect(input).toHaveAttribute("placeholder", "0");
    expect(total()).toHaveTextContent("Total sale value: $0.00");
  });

  test("shows a message when there are no herds", () => {
    renderModal({ herds: [] });
    expect(screen.getByText("You have no herds to sell.")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  test("defaults to no herds when herds are not loaded", () => {
    renderModal({ herds: undefined });
    expect(screen.getByText("You have no herds to sell.")).toBeInTheDocument();
  });

  test("total sale value uses each herd's own health and the selling fee", () => {
    renderModal();
    typeCount(11, "2");
    // 2 * (100 * 100% - 2) = 196
    expect(total()).toHaveTextContent("Total sale value: $196.00");
    typeCount(12, "1");
    // + 1 * (100 * 62.345% - 2) = 60.345 -> 256.35
    expect(total()).toHaveTextContent("Total sale value: $256.35");
  });

  test("a missing selling fee counts as 0", () => {
    renderModal({ game: { cowPrice: 10 } });
    typeCount(11, "3");
    expect(total()).toHaveTextContent("Total sale value: $30.00");
  });

  test("entries are capped at the herd's size", () => {
    renderModal();
    typeCount(11, "5");
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(3);
    typeCount(11, "3");
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(3);
  });

  test("negative entries become 0", () => {
    renderModal();
    typeCount(11, "-2");
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(0);
    typeCount(11, "0");
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(0);
  });

  test("clearing an entry leaves it empty", () => {
    renderModal();
    typeCount(11, "2");
    typeCount(11, "");
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(null);
    expect(total()).toHaveTextContent("Total sale value: $0.00");
  });

  test("sells only the herds with a count, then closes", () => {
    renderModal();
    typeCount(11, "3");
    typeCount(12, "0");
    fireEvent.click(screen.getByTestId("sell-herds-modal-submit"));
    expect(mockOnSell).toHaveBeenCalledWith({ 11: 3 });
    expect(mockToastError).not.toHaveBeenCalled();
    expect(mockOnClose).toHaveBeenCalled();
  });

  test("sells from several herds at once", () => {
    renderModal();
    typeCount(11, "1");
    typeCount(12, "4");
    fireEvent.click(screen.getByTestId("sell-herds-modal-submit"));
    expect(mockOnSell).toHaveBeenCalledWith({ 11: 1, 12: 4 });
  });

  test("selling nothing closes the window with an error message", () => {
    renderModal();
    fireEvent.click(screen.getByTestId("sell-herds-modal-submit"));
    expect(mockToastError).toHaveBeenCalledWith(
      "You must sell at least 1 cow.",
    );
    expect(mockOnSell).not.toHaveBeenCalled();
    expect(mockOnClose).toHaveBeenCalled();
  });

  test("a fractional entry closes the window with an error message", () => {
    renderModal();
    typeCount(11, "1");
    typeCount(12, "1.5");
    fireEvent.click(screen.getByTestId("sell-herds-modal-submit"));
    expect(mockToastError).toHaveBeenCalledWith(
      "You cannot sell a fractional number of cows! Please enter a whole number.",
    );
    expect(mockOnSell).not.toHaveBeenCalled();
    expect(mockOnClose).toHaveBeenCalled();
  });

  test("pressing Enter in an input submits the form", () => {
    renderModal();
    typeCount(11, "2");
    fireEvent.submit(screen.getByTestId("sell-herds-modal-input-11"));
    expect(mockOnSell).toHaveBeenCalledWith({ 11: 2 });
  });

  test("cancel closes without selling and clears the entries", () => {
    const { rerender } = renderModal();
    typeCount(11, "2");
    fireEvent.click(screen.getByTestId("sell-herds-modal-cancel"));
    expect(mockOnClose).toHaveBeenCalled();
    expect(mockOnSell).not.toHaveBeenCalled();
    rerender(
      <SellHerdsModal
        isOpen={true}
        onClose={mockOnClose}
        herds={herds}
        game={game}
        onSell={mockOnSell}
      />,
    );
    expect(screen.getByTestId("sell-herds-modal-input-11")).toHaveValue(null);
  });

  test("the close (X) button closes without selling", () => {
    renderModal();
    fireEvent.click(screen.getByRole("button", { name: "Close" }));
    expect(mockOnClose).toHaveBeenCalled();
    expect(mockOnSell).not.toHaveBeenCalled();
  });
});
