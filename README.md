# Kuramoto Lab

**Watch order emerge from chaos.**

Kuramoto Lab is a small desktop program that lets you watch thousands of independent "clocks" fall into step with one another, all by themselves, with no conductor telling them what to do. Drag a slider and you can see the exact moment disorder gives way to harmony.

![Kuramoto Lab in action](images/screenshot.png)

---

## What is this about?

Nature is full of things that tick, pulse or flash at their own natural rhythm:

- fireflies flashing in a tree,
- pacemaker cells in the heart,
- neurons in the brain,
- crickets chirping in a field,
- an audience clapping after a concert,
- pendulum clocks hanging on the same wall.

Left alone, each one keeps its own time, and together they produce noise. But when they can *influence* one another, even weakly, something remarkable happens. Above a certain strength of influence, a group of them suddenly locks together and starts pulsing as one. Thousands of fireflies along a riverbank in Southeast Asia really do flash in unison.

In 1975 the Japanese physicist **Yoshiki Kuramoto** found a beautifully simple mathematical description of this. His model has only two ingredients:

1. **Every oscillator has its own natural speed.** Some are a little faster than average, some a little slower.
2. **Every oscillator is nudged towards the others.** If it is running ahead of the crowd it gets pulled back slightly; if it is lagging behind it gets pushed forward.

The strength of that nudging is called the **coupling**. Kuramoto showed that synchrony does not grow gradually. Below a critical coupling there is essentially none. Cross that threshold and order appears spontaneously, and grows the further past it you go. It is one of the clearest examples in science of **emergence**: a collective behaviour that no individual member is aiming for.

Kuramoto Lab lets you see this with your own eyes, and checks it against Kuramoto's exact mathematical prediction.

---

## Getting started

You need **Java** (version 11 or newer) installed on your computer. It is free.

1. **Check whether you already have Java.** Open *Terminal* (on a Mac) or *Command Prompt* (on Windows) and type:

   ```
   java -version
   ```

   If you see a version number of 11 or higher, you are ready. If not, download Java from [adoptium.net](https://adoptium.net) and install it like any other program.

2. **Download Kuramoto Lab.** On this page, click the green **Code** button and choose **Download ZIP**, then unzip it. (Or download just the file `KuramotoLab.java`.)

3. **Run it.** In Terminal, go to the folder where the file is and type:

   ```
   java KuramotoLab.java
   ```

   A window will open and the simulation starts straight away. There is nothing else to install.

---

## The two worlds

Use the drop-down menu at the top right to switch between two different worlds.

### 1. The Ring: everyone hears everyone

This is Kuramoto's original model. 1,200 oscillators all feel the pull of the whole crowd, rather like an audience where everyone can hear everyone else's clapping.

How to read the picture:

- **Each dot is one oscillator.** Its position *around* the circle shows where it is in its cycle (think of it as the hand of a clock).
- **Its distance from the centre shows its natural speed.** Slow ones (blue) sit near the middle; fast ones (red) sit near the outside.
- **The white arrow shows how much the crowd agrees.** When the dots are scattered all around, their pulls cancel out and the arrow is tiny. When they bunch up, the arrow grows long and points to where the crowd is.

The length of that arrow is called the **order parameter**, written *r*. It runs from 0 (total disorder) to 1 (perfect unison), and is shown as a live number and a graph on the right.

**What to try:**

1. Set **Coupling K** to about 0.5. The dots spin round independently and the arrow just jitters. This is disorder.
2. Now slowly drag **Coupling K** to the right. Watch closely as you pass **1.00**. A smooth, bright arc of dots suddenly condenses out of the scatter and freezes in place, and the arrow begins to grow.
3. Notice that the arc is made of the *middle* speeds (green and yellow). The very fastest and very slowest oscillators keep drifting around on their own. Synchrony takes hold at the centre of the crowd first and spreads outward as the coupling strengthens.
4. Press **Scramble** to throw everyone into random positions again and watch the order rebuild itself.

### 2. The Lattice: everyone hears only their neighbours

Here 25,600 oscillators sit on a grid, and each one only feels its four immediate neighbours, rather like people in a crowd who can only see the person next to them. The colour of each square shows where that oscillator is in its cycle.

**What to try:**

1. Press **Scramble**. The grid becomes random coloured static.
2. Within seconds, patches of matching colour start to form and grow, like frost spreading on a window.
3. Look for the special points where **every colour of the rainbow meets**. These are **vortices**, small whirlpools in the rhythm that the oscillators cannot smooth away on their own. Each one turns either clockwise or anticlockwise.
4. Watch them wander. When a clockwise vortex meets an anticlockwise one, the two annihilate each other and the colours smooth over. Order spreads not all at once, but by the slow elimination of defects.
5. Raise **Spread γ** a little and the patches start to compete, each trying to impose its own rhythm on its neighbours.

![The lattice: domains of matching rhythm, with vortices where all colours meet](images/lattice.png)

---

## The controls

| Control | What it does |
|---|---|
| **Coupling K** | How strongly each oscillator is pulled towards the others. This is the main dial. |
| **Spread γ** | How different the oscillators' natural speeds are. A bigger spread means a more varied, harder-to-tame crowd. |
| **Noise D** | Random jostling, like a shaky hand. Noise works *against* synchrony. |
| **Steps / frame** | Simulation speed. Higher is faster. |
| **Scramble** | Throws every oscillator into a random position and starts again. |
| **Pause / Run** | Freezes the picture so you can look closely. |
| **Sweep K** | Runs a scientific experiment (see below). Available in the Ring world. |

The **status line** shows three numbers: *r*, the current amount of order; *Kc*, the critical coupling where synchrony should begin; and *K/Kc*, how far past that threshold you are.

---

## Checking the theory: the Sweep

Kuramoto did not just describe this behaviour. He predicted it *exactly*. For oscillators like these, he showed that:

- synchrony begins at a critical coupling **Kc = 2 × Spread**, and
- above that point, the amount of order follows the curve **r = √(1 − Kc / K)**.

Press **Sweep K** and the program runs a separate experiment with 2,000 oscillators, turning the coupling up step by step and measuring how much order settles in at each setting. The results appear as **white dots** on the lower graph, alongside Kuramoto's predicted curve in **blue**.

They agree to within about one percent. A flat line of near-zero order, a sharp kink at the threshold, and then a steady climb: half a century after it was written down, the equation still holds.

The **orange line** marks where your Coupling slider is set, so you can see where you are on the curve.

Try adding some **Noise** before sweeping. The threshold shifts to the right (to **2 × (Spread + Noise)**): random jostling makes it harder for the crowd to agree.

---

## Why it matters

The same mathematics turns up in a surprising range of places:

- **The brain.** Rhythms such as alpha and gamma waves arise when large populations of neurons fall partly into step. Too little synchrony and the brain cannot coordinate; too much, and the result is an epileptic seizure.
- **The heart.** Thousands of pacemaker cells must agree on a single beat.
- **Power grids.** Generators across a country must all spin in step at the same frequency.
- **Electronics.** Oscillators "lock" onto each other in radios and clock circuits.
- **Biology and society.** Firefly displays, cricket choruses, applause, and even crowds unintentionally falling into step on a wobbly footbridge (London's Millennium Bridge in 2000).

A lesson of the Kuramoto model is that order does not need a leader. It can arise from many independent parts, each following a simple local rule, once their connection to one another is strong enough.

---

## Technical note

Kuramoto Lab is a single Java file with no external libraries. In the Ring world each oscillator's phase θ obeys

  dθ/dt = ω + K · r · sin(ψ − θ) + noise

where ω is its natural frequency (drawn from a Lorentzian distribution with half-width γ) and r·e^{iψ} is the average of all the oscillators' positions on the unit circle. In the Lattice world each cell instead feels the average of sin(θ_neighbour − θ) over its four neighbours, on a 160 × 160 grid that wraps around at the edges.

---

## Author

Created by **Mario Gianota** ([RDMachinery](https://github.com/RDMachinery)).
