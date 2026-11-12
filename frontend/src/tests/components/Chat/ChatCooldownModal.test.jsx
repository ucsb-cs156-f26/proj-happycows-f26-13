import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { vi, describe, test, beforeEach, expect } from "vitest";
import "@testing-library/jest-dom";
import ChatCooldownModal from "main/components/Chat/ChatCooldownModal";

describe("ChatCooldownModal", () => {
  const onClose = vi.fn();
  const onConfirm = vi.fn();

  const renderModal = () =>
    render(
      <ChatCooldownModal
        username="Alice"
        onClose={onClose}
        onConfirm={onConfirm}
      />,
    );

  const setAmount = (value) =>
    fireEvent.change(screen.getByTestId("ChatCooldownModal-amount"), {
      target: { value },
    });

  const setUnit = (value) =>
    fireEvent.change(screen.getByTestId("ChatCooldownModal-unit"), {
      target: { value },
    });

  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("renders title, label, defaults, and layout", () => {
    renderModal();

    expect(screen.getByTestId("ChatCooldownModal")).toBeInTheDocument();
    expect(screen.getByText("Chat cooldown for Alice")).toBeInTheDocument();

    const amount = screen.getByLabelText(
      "How long should this user be unable to send chat messages?",
    );
    expect(amount).toHaveValue(5);
    expect(amount).toHaveAttribute("type", "number");
    expect(amount).toHaveAttribute("min", "0");
    expect(amount).toHaveAttribute("step", "1");
    expect(amount).not.toHaveClass("is-invalid");
    expect(amount.parentElement).toHaveClass("d-flex", "gap-2");

    const unit = screen.getByLabelText("Cooldown unit");
    expect(unit).toHaveValue("minutes");
    expect(
      Array.from(unit.querySelectorAll("option")).map((o) => [
        o.value,
        o.textContent,
      ]),
    ).toEqual([
      ["seconds", "seconds"],
      ["minutes", "minutes"],
      ["hours", "hours"],
    ]);

    expect(
      screen.queryByTestId("ChatCooldownModal-error"),
    ).not.toBeInTheDocument();

    const cancel = screen.getByTestId("ChatCooldownModal-cancel");
    expect(cancel).toHaveTextContent("Cancel");
    expect(cancel).toHaveClass("btn-secondary");

    const confirm = screen.getByTestId("ChatCooldownModal-confirm");
    expect(confirm).toHaveTextContent("Start cooldown");
    expect(confirm).toHaveClass("btn-warning");
    expect(confirm).toBeEnabled();
  });

  test("confirm with defaults sends 5 minutes (300 seconds) and closes", () => {
    renderModal();
    fireEvent.click(screen.getByTestId("ChatCooldownModal-confirm"));
    expect(onConfirm).toHaveBeenCalledWith(300);
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  test("converts seconds and hours", () => {
    renderModal();

    setAmount("30");
    setUnit("seconds");
    fireEvent.click(screen.getByTestId("ChatCooldownModal-confirm"));
    expect(onConfirm).toHaveBeenLastCalledWith(30);

    setAmount("2");
    setUnit("hours");
    fireEvent.click(screen.getByTestId("ChatCooldownModal-confirm"));
    expect(onConfirm).toHaveBeenLastCalledWith(7200);
  });

  test.each(["-1", "1.5", ""])(
    "invalid amount %j shows error and disables confirm",
    (value) => {
      renderModal();
      setAmount(value);

      const error = screen.getByTestId("ChatCooldownModal-error");
      expect(error).toHaveTextContent(
        "Enter a whole number 0 or greater (0 clears the cooldown).",
      );
      expect(error).toHaveClass("text-danger", "mt-1");
      expect(screen.getByTestId("ChatCooldownModal-amount")).toHaveClass(
        "is-invalid",
      );

      const confirm = screen.getByTestId("ChatCooldownModal-confirm");
      expect(confirm).toBeDisabled();
      fireEvent.click(confirm);
      expect(onConfirm).not.toHaveBeenCalled();
    },
  );

  test("smallest valid amount (1) is accepted", () => {
    renderModal();
    setAmount("1");
    expect(
      screen.queryByTestId("ChatCooldownModal-error"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("ChatCooldownModal-confirm"));
    expect(onConfirm).toHaveBeenCalledWith(60);
  });

  test("0 is accepted and clears the cooldown", () => {
    renderModal();
    setAmount("0");
    expect(
      screen.queryByTestId("ChatCooldownModal-error"),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("ChatCooldownModal-confirm"));
    expect(onConfirm).toHaveBeenCalledWith(0);
  });

  test("cancel closes without confirming", () => {
    renderModal();
    fireEvent.click(screen.getByTestId("ChatCooldownModal-cancel"));
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  test("header close button closes without confirming", () => {
    renderModal();
    fireEvent.click(screen.getByLabelText("Close"));
    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
