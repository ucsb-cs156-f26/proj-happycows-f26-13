import React from "react";
import { Alert, Button, Card, Table } from "react-bootstrap";

export default function HerdList({ herds = [], isLoading, isError, onRetry }) {
  let content;
  if (isLoading) {
    content = <p role="status">Loading herds...</p>;
  } else if (isError) {
    content = (
      <Alert variant="danger">
        <p>Unable to load your herds.</p>
        <Button variant="outline-danger" onClick={() => onRetry()}>
          Retry
        </Button>
      </Alert>
    );
  } else {
    const totalCows = herds.reduce((total, herd) => total + herd.numCows, 0);
    content = (
      <>
        <p>Total cows in herds: {totalCows}</p>
        {herds.length === 0 ? (
          <p>You have no herds yet.</p>
        ) : (
          <Table responsive striped bordered aria-label="Your herds">
            <thead>
              <tr>
                <th scope="col">Herd</th>
                <th scope="col">Cows</th>
                <th scope="col">Health</th>
              </tr>
            </thead>
            <tbody>
              {herds.map((herd) => (
                <tr key={herd.id}>
                  <th scope="row">Herd #{herd.herdNumber}</th>
                  <td>{herd.numCows}</td>
                  <td>{herd.health.toFixed(2)}%</td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </>
    );
  }
  return (
    <Card className="my-3" data-testid="HerdList">
      <Card.Header as="h5" className="woodenboardtable">
        Your Herds
      </Card.Header>
      <Card.Body>{content}</Card.Body>
    </Card>
  );
}
