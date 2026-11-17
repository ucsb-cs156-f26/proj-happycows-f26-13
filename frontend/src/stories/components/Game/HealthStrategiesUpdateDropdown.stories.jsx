import React from "react";
import HealthStrategiesUpdateDropdown from "main/components/Game/HealthStrategiesUpdateDropdown";
import healthUpdateStrategyListFixtures from "fixtures/healthUpdateStrategyListFixtures";

export default {
  title: "components/Game/HealthStrategiesUpdateDropdown",
  component: HealthStrategiesUpdateDropdown,
};

const Template = (args) => {
  const register = (fieldName) => ({
    name: fieldName,
    defaultValue: args.initialValue,
  });

  return <HealthStrategiesUpdateDropdown {...args} register={register} />;
};

export const SimpleStrategies = Template.bind({});

SimpleStrategies.args = {
  formName: "belowCapacityStrategy",
  displayName: "Below Capacity Strategy",
  initialValue: "strat1",
  healthUpdateStrategies: healthUpdateStrategyListFixtures.simple,
};

export const RealStrategies = Template.bind({});

RealStrategies.args = {
  formName: "aboveCapacityStrategy",
  displayName: "Above Capacity Strategy",
  initialValue: "Linear",
  healthUpdateStrategies: healthUpdateStrategyListFixtures.real,
};
