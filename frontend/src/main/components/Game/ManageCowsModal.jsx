import React from "react";
import { Modal, Button, Form } from "react-bootstrap";
import { toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";

const ManageCowsModal = ({
  number,
  setNumber,
  isOpen,
  onClose,
  message,
  farmer,
  game,
  onBuy,
  onSell,
}) => {
  const handleInputChange = (e) => {
    const value = e.target.value;
    if (value < 0) {
      toast.warn(`Warning: You cannot ${message} a negative number of cows!`);
    } else if (!Number.isInteger(Number(value))) {
      toast.warn(
        `Warning: You cannot ${message} a fractional number of cows! Please enter a whole number.`,
      );
    }
    setNumber(value);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (number < 0) {
      window.alert(
        `You cannot ${message} a negative number of cows! Please enter a non-negative number.`,
      );
    } else if (!Number.isInteger(Number(number))) {
      window.alert(
        `You cannot ${message} a fractional number of cows! Please enter a whole number.`,
      );
    } else if (message.includes("buy") && Number(number) < 1) {
      toast.error("You must buy at least 1 cow.");
      setNumber(1);
      onClose();
    } else {
      if (message.includes("buy")) {
        onBuy(farmer, number);
      } else {
        onSell(farmer, number);
      }
      setNumber(1);
      onClose(); // Close the modal
    }
  };

  const handleClose = () => {
    setNumber(1);
    onClose();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      data-testid={"buy-sell-cow-modal"}
      show={isOpen}
      onHide={handleClose}
    >
      <Modal.Body>
        <Form onSubmit={handleSubmit}>
          <Form.Group
            data-testid={"buy-sell-cow-modal-closeGroup"}
            style={{ display: "flex", justifyContent: "right" }}
          >
            <Button
              data-testid={"buy-sell-cow-modal-close"}
              style={{
                color: "#000",
                borderColor: "#fff",
                backgroundColor: "#fff",
              }}
              type="button"
              className="close"
              aria-label="Close"
              onClick={handleClose}
            >
              <span aria-hidden="true">&times;</span>
            </Button>
          </Form.Group>
          <Form.Group>
            <Form.Label>
              Please specify the number of cows you'd like to {message} below.
            </Form.Label>
          </Form.Group>
          <Form.Group>
            <Form.Control
              data-testid={"buy-sell-cow-modal-input"}
              style={{ width: "20%" }}
              type="number"
              value={number}
              onChange={handleInputChange}
              min="1"
            />
          </Form.Group>
          {message.includes("buy") && game && (
            <Form.Text data-testid={"buy-sell-cow-modal-total-cost"}>
              Total cost: $
              {(
                (Number(number) || 0) *
                (game.cowPrice + (game.purchaseFee ?? 0))
              ).toFixed(2)}
            </Form.Text>
          )}
        </Form>
      </Modal.Body>
      <Modal.Footer
        data-testid={"buy-sell-cow-modal-footer"}
        style={{ borderTop: "0px" }}
      >
        <Button
          data-testid={"buy-sell-cow-modal-cancel"}
          variant="secondary"
          onClick={handleClose}
        >
          Cancel
        </Button>
        <Button
          data-testid={"buy-sell-cow-modal-submit"}
          variant="primary"
          onClick={handleSubmit}
        >
          {message[0].toUpperCase() + message.slice(1)}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default ManageCowsModal;
