import BackgroundDefault from "../../assets/PlayPageBackground.jpg";
import BackgroundSmiling from "../../assets/PlayPageBackground-Smiling.jpg";
import BackgroundNeutral from "../../assets/PlayPageBackground-Neutral.jpg";
import BackgroundFrowning from "../../assets/PlayPageBackground-Frowning.jpg";
import BackgroundDead from "../../assets/PlayPageBackground-Dead.jpg";

// Picks the PlayPage background from the farmer's average cow health (the
// same `farmer.cowHealth` value shown in FarmStats). The lower bound of each
// range is inclusive:
//   75-100 smiling, 50-75 neutral, 25-50 frowning, 0-25 dead (x's over eyes).
// If health is not known (e.g. the farmer is still loading), the original
// background is used.
export const getPlayPageBackground = (cowHealth) => {
  if (cowHealth == null || Number.isNaN(Number(cowHealth))) {
    return BackgroundDefault;
  }
  const health = Number(cowHealth);
  if (health >= 75) return BackgroundSmiling;
  if (health >= 50) return BackgroundNeutral;
  if (health >= 25) return BackgroundFrowning;
  return BackgroundDead;
};
