import { useState } from "react";
import { Button, Form, Table } from "react-bootstrap";
import BasicLayout from "main/layouts/BasicLayout/BasicLayout";
import { useBackend, useBackendMutation } from "main/utils/useBackend";

const endpoint = "/api/admin/bannedwords/all";

export default function AdminBannedWordsPage() {
  const [word, setWord] = useState("");

  const { data: bannedWords } = useBackend(
    [endpoint],
    { method: "GET", url: endpoint },
    [],
  );

  const addMutation = useBackendMutation(
    (value) => ({
      method: "POST",
      url: "/api/admin/bannedwords/post",
      params: { word: value },
    }),
    { onSuccess: () => setWord("") },
    [endpoint],
  );

  const deleteMutation = useBackendMutation(
    (id) => ({
      method: "DELETE",
      url: "/api/admin/bannedwords",
      params: { id },
    }),
    {},
    [endpoint],
  );

  const submitWord = (event) => {
    event.preventDefault();
    addMutation.mutate(word);
  };

  return (
    <BasicLayout>
      <h2>Banned Words</h2>

      <Form onSubmit={submitWord}>
        <Form.Group className="mb-3">
          <Form.Label htmlFor="banned-word">Word</Form.Label>
          <Form.Control
            id="banned-word"
            value={word}
            onChange={(event) => setWord(event.target.value)}
          />
        </Form.Group>
        <Button type="submit" className="mb-3">
          Add
        </Button>
      </Form>

      <Table striped bordered>
        <thead>
          <tr>
            <th>Word</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {bannedWords.map((entry) => (
            <tr key={entry.id}>
              <td>{entry.word}</td>
              <td>
                <Button
                  variant="danger"
                  onClick={() => deleteMutation.mutate(entry.id)}
                  aria-label={`Delete ${entry.word}`}
                >
                  Delete
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </BasicLayout>
  );
}
