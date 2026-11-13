import React from "react";
import { render, fireEvent, screen, waitFor } from "@testing-library/react";
import ManageCowsModal from "main/components/Game/ManageCowsModal";
import farmerFixtures from "fixtures/farmerFixtures";
import { vi } from "vitest";
import "@testing-library/jest-dom";

const mockToast = vi.fn();
const BUY_NEG_WARN = "Warning: You cannot buy a negative number of cows!";
const SELL_NEG_WARN = "Warning: You cannot sell a negative number of cows!";
const BUY_FRAC_WARN =
  "Warning: You cannot buy a fractional number of cows! Please enter a whole number.";
const SELL_FRAC_WARN =
  "Warning: You cannot sell a fractional number of cows! Please enter a whole number.";
vi.mock("react-toastify", async () => {
  const originalModule = await vi.importActual("react-toastify");
  return {
    __esModule: true,
    ...originalModule,
    toast: {
      ...originalModule.toast,
      warn: (message) => mockToast(message),
    },
  };
});

describe("ManageCowsModal", () => {
  const mockOnClose = vi.fn();
  const mockSetNumber = vi.fn();
  const mockOnBuy = vi.fn();
  const mockOnSell = vi.fn();
  window.alert = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("toast warning not present when buying 0 cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");
    fireEvent.change(input, { target: { value: "0" } });

    expect(BUY_NEG_WARN.length).toBeGreaterThan(0);
    expect(BUY_FRAC_WARN.length).toBeGreaterThan(0);
    expect(mockSetNumber).toHaveBeenCalledWith("0");
    expect(mockToast).not.toHaveBeenCalledWith(BUY_NEG_WARN);
    expect(mockToast).not.toHaveBeenCalledWith(BUY_FRAC_WARN);
  });

  test("toast warning not present when selling 0 cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");
    fireEvent.change(input, { target: { value: "0" } });

    expect(SELL_NEG_WARN.length).toBeGreaterThan(0);
    expect(SELL_FRAC_WARN.length).toBeGreaterThan(0);
    expect(mockSetNumber).toHaveBeenCalledWith("0");
    expect(mockToast).not.toHaveBeenCalledWith(SELL_NEG_WARN);
    expect(mockToast).not.toHaveBeenCalledWith(SELL_FRAC_WARN);
  });

  test("toast warning for inputing fractional number when buying cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");
    fireEvent.change(input, { target: { value: "1.5" } });

    expect(mockSetNumber).toHaveBeenCalledWith("1.5");
    expect(mockToast).toHaveBeenCalledWith(
      "Warning: You cannot buy a fractional number of cows! Please enter a whole number.",
    );
  });

  test("toast warning for inputing fractional number when selling cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");
    fireEvent.change(input, { target: { value: "2.5" } });

    expect(mockSetNumber).toHaveBeenCalledWith("2.5");
    expect(mockToast).toHaveBeenCalledWith(
      "Warning: You cannot sell a fractional number of cows! Please enter a whole number.",
    );
  });

  test("window alert warning for buying fractional number of cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        farmer={farmerFixtures.oneFarmer[0]}
        number={1.5}
        onBuy={mockOnBuy}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    expect(window.alert).toHaveBeenCalledWith(
      "You cannot buy a fractional number of cows! Please enter a whole number.",
    );
    expect(mockOnBuy).not.toHaveBeenCalled();
  });

  test("window alert warning for selling fractional number of cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        farmer={farmerFixtures.oneFarmer[0]}
        number={2.5}
        onSell={mockOnSell}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    expect(window.alert).toHaveBeenCalledWith(
      "You cannot sell a fractional number of cows! Please enter a whole number.",
    );
    expect(mockOnSell).not.toHaveBeenCalled();
  });

  test("renders the modal when isOpen is true", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    expect(screen.getByTestId("buy-sell-cow-modal")).toBeInTheDocument();
  });

  test("modal has correct buy capitalization", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    expect(screen.getByText("Buy")).toBeInTheDocument();
  });

  test("modal has correct sell capitalization", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        setNumber={mockSetNumber}
      />,
    );

    expect(screen.getByText("Sell")).toBeInTheDocument();
  });

  test("does not render the modal when isOpen is false", () => {
    render(
      <ManageCowsModal
        isOpen={false}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    expect(screen.queryByTestId("buy-sell-cow-modal")).not.toBeInTheDocument();
  });

  test("calls onClose and setNumber when close button is clicked", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-close"));
    expect(mockOnClose).toHaveBeenCalled();
    expect(mockSetNumber).toHaveBeenCalledWith(1);
  });

  test("calls onClose and setNumber when cancel button is clicked", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-cancel"));
    expect(mockOnClose).toHaveBeenCalled();
    expect(mockSetNumber).toHaveBeenCalledWith(1);
  });

  test("calls onBuy with correct arguments when buy button is clicked", async () => {
    const number = 5;
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        farmer={farmerFixtures.oneFarmer[0]}
        number={number}
        onBuy={mockOnBuy}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    await waitFor(() =>
      expect(mockOnBuy).toHaveBeenCalledWith(
        farmerFixtures.oneFarmer[0],
        number,
      ),
    );
  });

  test("calls onSell with correct arguments when sell button is clicked", async () => {
    const number2 = 3;
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        farmer={farmerFixtures.oneFarmer[0]}
        number={number2}
        onSell={mockOnSell}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    await waitFor(() =>
      expect(mockOnSell).toHaveBeenCalledWith(
        farmerFixtures.oneFarmer[0],
        number2,
      ),
    );
  });

  test("calls onSell with correct arguments when sell button is clicked (0 argument)", async () => {
    const number2 = 0;
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        farmer={farmerFixtures.oneFarmer[0]}
        number={number2}
        onSell={mockOnSell}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    await waitFor(() =>
      expect(mockOnSell).toHaveBeenCalledWith(
        farmerFixtures.oneFarmer[0],
        number2,
      ),
    );
  });

  test("windows toolkit warning for buying negative cows", async () => {
    const number2 = -3;

    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        farmer={farmerFixtures.oneFarmer[0]}
        number={number2}
        onBuy={mockOnBuy}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    expect(window.alert).toHaveBeenCalledWith(
      "You cannot buy a negative number of cows! Please enter a non-negative number.",
    );
  });

  test("windows toolkit warning for selling negative cows", async () => {
    const number2 = -3;

    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        farmer={farmerFixtures.oneFarmer[0]}
        number={number2}
        onSell={mockOnSell}
        setNumber={mockSetNumber}
      />,
    );

    fireEvent.click(screen.getByTestId("buy-sell-cow-modal-submit"));

    expect(window.alert).toHaveBeenCalledWith(
      "You cannot sell a negative number of cows! Please enter a non-negative number.",
    );
  });

  test("toast warning for inputing negative number when buying cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");

    fireEvent.change(input, { target: { value: "-3" } });
    expect(mockSetNumber).toHaveBeenCalledWith("-3");
    expect(mockToast).toHaveBeenCalledTimes(1);
    const msg = "Warning: You cannot buy a negative number of cows!";
    expect(msg.length).toBeGreaterThan(0);
    expect(mockToast).toHaveBeenCalledWith(msg);
  });

  test("toast warning for inputing negative number when selling cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");

    fireEvent.change(input, { target: { value: "-3" } });
    expect(mockSetNumber).toHaveBeenCalledWith("-3");
    expect(mockToast).toHaveBeenCalledTimes(1);
    const msg = "Warning: You cannot sell a negative number of cows!";
    expect(msg.length).toBeGreaterThan(0);
    expect(mockToast).toHaveBeenCalledWith(msg);
  });

  test("no negative warning toast when buying 0 cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");

    fireEvent.change(input, { target: { value: "0" } });
    expect(mockSetNumber).toHaveBeenCalledWith("0");
    expect(mockToast).not.toHaveBeenCalledWith(
      "Warning: You cannot buy a negative number of cows!",
    );
  });

  test("no negative warning toast when selling 0 cows", async () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="sell"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");

    fireEvent.change(input, { target: { value: "0" } });
    expect(mockSetNumber).toHaveBeenCalledWith("0");
    expect(mockToast).not.toHaveBeenCalledWith(
      "Warning: You cannot sell a negative number of cows!",
    );
  });

  test("updates the number state on input change", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    const input = screen.getByTestId("buy-sell-cow-modal-input");

    fireEvent.change(input, { target: { value: "10" } });
    expect(mockSetNumber).toHaveBeenCalledWith("10");
  });

  test("Check styling of modal components", () => {
    render(
      <ManageCowsModal
        isOpen={true}
        onClose={mockOnClose}
        message="buy"
        setNumber={mockSetNumber}
      />,
    );

    expect(screen.getByTestId("buy-sell-cow-modal-footer")).toHaveStyle(
      "border-top-width: 0px",
    );
    expect(screen.getByTestId("buy-sell-cow-modal-input")).toHaveStyle(
      "width: 20%",
    );
    expect(screen.getByTestId("buy-sell-cow-modal-close")).toHaveStyle(
      "color: rgb(0, 0, 0); border-color: #fff; background-color: rgb(255, 255, 255);",
    );
    expect(screen.getByTestId("buy-sell-cow-modal-close").style.color).toBe(
      "rgb(0, 0, 0)",
    );
    expect(screen.getByTestId("buy-sell-cow-modal-closeGroup")).toHaveStyle(
      "display: flex; justify-content: right",
    );
  });
});
