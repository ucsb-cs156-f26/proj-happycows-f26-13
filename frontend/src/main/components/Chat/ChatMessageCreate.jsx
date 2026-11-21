import React from "react";
import { Button, Form } from "react-bootstrap";
import { useForm } from "react-hook-form";
import { toast } from "react-toastify";

import { useBackendMutation } from "main/utils/useBackend";

const ChatMessageCreate = ({ gameId, submitAction }) => {
  const testid = "ChatMessageCreate";
  const initialMessagePageSize = 10;

  const objectToAxiosParams = (newMessage) => ({
    // Stryker disable next-line all : axiosMock post test works when mutated
    url: `/api/chat/post?gameId=${newMessage.gameId}&content=${newMessage.content}`,
    method: "POST",
    data: newMessage,
  });

  // The backend includes bannedWord and cooldownSeconds only when the message
  // contained a banned word and the sender was just placed on a chat cooldown.
  const onSuccess = (response) => {
    if (response.bannedWord) {
      toast.warn(
        `Your chat message included a banned word ("${response.bannedWord}"). ` +
          `You have been placed on chat cooldown for the next ${response.cooldownSeconds} seconds.`,
      );
    }
  };

  const mutation = useBackendMutation(
    objectToAxiosParams,
    { onSuccess },
    // Stryker disable all : hard to set up test for caching
    [`/api/chat/get?page=0&size=${initialMessagePageSize}&gameId=${gameId}`],
    // Stryker restore all
  );

  submitAction =
    submitAction ||
    (async (data) => {
      const escapedContent = encodeURIComponent(data.message);
      const escapedGameId = encodeURIComponent(Number(gameId));
      const params = { content: escapedContent, gameId: escapedGameId };
      mutation.mutate(params);
      reset();
    });

  const {
    register,
    formState: { errors },
    handleSubmit,
    reset,
  } = useForm();

  return (
    <Form
      data-testid={testid}
      onSubmit={handleSubmit(submitAction)}
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
      }}
    >
      <Form.Control
        data-testid={`${testid}-Message`}
        id="message"
        type="text"
        {...register("message", { required: "Message cannot be empty" })}
      />
      <Form.Control.Feedback type="invalid">
        {errors.message?.message}
      </Form.Control.Feedback>
      <Button type="submit" data-testid={`${testid}-Send`}>
        Send
      </Button>
    </Form>
  );
};

export default ChatMessageCreate;
