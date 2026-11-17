import React from "react";
import ChatCooldownModal from "main/components/Chat/ChatCooldownModal";

export default {
  title: "components/Chat/ChatCooldownModal",
  component: ChatCooldownModal,
};

const Template = (args) => <ChatCooldownModal {...args} />;

export const Default = Template.bind({});

Default.args = {
  username: "Alice",
  onClose: () => {},
  onConfirm: (seconds) => window.alert(`Cooldown for ${seconds} seconds`),
};
