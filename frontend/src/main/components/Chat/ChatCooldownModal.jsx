import React, { useState } from "react";
import { Modal, Button, Form } from "react-bootstrap";

const UNIT_SECONDS = { seconds: 1, minutes: 60, hours: 3600 };

const ChatCooldownModal = ({ username, onClose, onConfirm }) => {
  const [amount, setAmount] = useState("5");
  const [unit, setUnit] = useState("minutes");

  const parsed = Number(amount);
  // Number("") is 0, so reject empty input explicitly
  const isValid = amount !== "" && Number.isInteger(parsed) && parsed >= 0;

  const handleConfirm = () => {
    onConfirm(parsed * UNIT_SECONDS[unit]);
    onClose();
  };

  return (
    <Modal show onHide={onClose} data-testid="ChatCooldownModal">
      <Modal.Header closeButton>
        <Modal.Title>Chat cooldown for {username}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Form.Label htmlFor="ChatCooldownModal-amount">
          How long should this user be unable to send chat messages?
        </Form.Label>
        <div className="d-flex gap-2">
          <Form.Control
            id="ChatCooldownModal-amount"
            data-testid="ChatCooldownModal-amount"
            type="number"
            min="0"
            step="1"
            value={amount}
            isInvalid={!isValid}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Form.Select
            aria-label="Cooldown unit"
            data-testid="ChatCooldownModal-unit"
            value={unit}
            onChange={(e) => setUnit(e.target.value)}
          >
            {Object.keys(UNIT_SECONDS).map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </Form.Select>
        </div>
        {!isValid && (
          <div
            className="text-danger mt-1"
            data-testid="ChatCooldownModal-error"
          >
            Enter a whole number 0 or greater (0 clears the cooldown).
          </div>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button
          variant="secondary"
          data-testid="ChatCooldownModal-cancel"
          onClick={onClose}
        >
          Cancel
        </Button>
        <Button
          variant="warning"
          data-testid="ChatCooldownModal-confirm"
          disabled={!isValid}
          onClick={handleConfirm}
        >
          Start cooldown
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default ChatCooldownModal;
