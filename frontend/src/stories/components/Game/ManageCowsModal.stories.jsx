import React from "react";
import ManageCowsModal from "main/components/Game/ManageCowsModal";

export default {
  title: "components/Game/ManageCowsModal",
  component: ManageCowsModal,
};

const Template = (args) => {
  const [isOpen, setIsOpen] = React.useState(true);

  const onBuy = (farmer, num) => {
    console.log(`Buy ${num} cows for farmer:`, farmer);
    args.onBuy(farmer, num);
    setIsOpen(false);
  };

  const onSell = (farmer, num) => {
    console.log(`Sell ${num} cows for farmer:`, farmer);
    args.onSell(farmer, num);
    setIsOpen(false);
  };

  return (
    <ManageCowsModal
      {...args}
      isOpen={isOpen}
      onClose={() => setIsOpen(false)}
      onBuy={onBuy}
      onSell={onSell}
    />
  );
};

export const BuyCows = Template.bind({});

BuyCows.args = {
  number: 1,
  message: "buy",
  farmer: { username: "testuser", gameId: 1 },
};

export const SellCows = Template.bind({});

SellCows.args = {
  number: 1,
  message: "sell",
  farmer: { username: "testuser", gameId: 1 },
};
