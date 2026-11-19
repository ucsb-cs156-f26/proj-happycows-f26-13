import React, { useState } from "react";
import { Modal, Button, Form, Table } from "react-bootstrap";
import { toast } from "react-toastify";

// What one cow from a herd sells for: the cow price scaled by the herd's
// health (as a percentage), minus the selling fee. Matches the backend.
const cowSaleValue = (game, health) =>
  (game.cowPrice * health) / 100 - (game.sellingFee ?? 0);

const SellHerdsModal = ({ isOpen, onClose, herds = [], game, onSell }) => {
  // Map of herd id -> what the user typed for that herd.
  const [counts, setCounts] = useState({});

  if (!isOpen) {
    return null;
  }

  const handleClose = () => {
    setCounts({});
    onClose();
  };

  // Keep each entry between 0 and the number of cows in that herd.
  const handleChange = (herd, value) => {
    let capped = value;
    if (value !== "" && Number(value) > herd.numCows) {
      capped = String(herd.numCows);
    } else if (value !== "" && Number(value) < 0) {
      capped = "0";
    }
    setCounts({ ...counts, [herd.id]: capped });
  };

  const countFor = (herd) => Number(counts[herd.id]) || 0;
  const totalCows = herds.reduce((sum, herd) => sum + countFor(herd), 0);
  const totalValue = herds.reduce(
    (sum, herd) => sum + countFor(herd) * cowSaleValue(game, herd.health),
    0,
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (herds.some((herd) => !Number.isInteger(countFor(herd)))) {
      toast.error(
        "You cannot sell a fractional number of cows! Please enter a whole number.",
      );
    } else if (totalCows < 1) {
      toast.error("You must sell at least 1 cow.");
    } else {
      const herdCounts = {};
      herds
        .filter((herd) => countFor(herd) > 0)
        .forEach((herd) => {
          herdCounts[herd.id] = countFor(herd);
        });
      onSell(herdCounts);
    }
    handleClose();
  };

  return (
    <Modal data-testid="sell-herds-modal" show={isOpen} onHide={handleClose}>
      <Modal.Header closeButton>
        <Modal.Title>Sell cows</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {herds.length === 0 ? (
          <p>You have no herds to sell.</p>
        ) : (
          <Form onSubmit={handleSubmit}>
            <Form.Label>
              Choose how many cows to sell from each herd.
            </Form.Label>
            <Table bordered size="sm" aria-label="Sell from your herds">
              <thead>
                <tr>
                  <th scope="col">Herd</th>
                  <th scope="col">Cows</th>
                  <th scope="col">Health</th>
                  <th scope="col">Sell</th>
                </tr>
              </thead>
              <tbody>
                {herds.map((herd) => (
                  <tr key={herd.id}>
                    <th scope="row">Herd #{herd.id}</th>
                    <td>{herd.numCows}</td>
                    <td>{herd.health.toFixed(2)}%</td>
                    <td>
                      <Form.Control
                        data-testid={`sell-herds-modal-input-${herd.id}`}
                        aria-label={`Cows to sell from Herd #${herd.id}`}
                        type="number"
                        min="0"
                        max={herd.numCows}
                        value={counts[herd.id] ?? ""}
                        placeholder="0"
                        onChange={(e) => handleChange(herd, e.target.value)}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Form.Text data-testid="sell-herds-modal-total">
              Total sale value: ${totalValue.toFixed(2)}
            </Form.Text>
          </Form>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button
          data-testid="sell-herds-modal-cancel"
          variant="secondary"
          onClick={handleClose}
        >
          Cancel
        </Button>
        <Button
          data-testid="sell-herds-modal-submit"
          variant="primary"
          onClick={handleSubmit}
        >
          Sell
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default SellHerdsModal;
