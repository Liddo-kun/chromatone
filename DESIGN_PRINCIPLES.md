# Design Principles

## Core Purpose
- A utility for playing noise, not an "app experience"
- Users primarily interact through hearing, not seeing
- Expected usage: select, play, put phone away
- Zero data collection, complete privacy
- Minimal battery and resource usage

## Design Guidelines

### 1. Invisible Design
- Interface should get out of the way
- No decorative elements
- Every visual element must serve a functional purpose
- Minimize cognitive load

### 2. Interface Structure
- Single screen application
- 3x2 grid for noise types
- Each noise type represented by:
  - Color-coded pill
  - Purpose text (Focus, Sleep, etc.)
- Minimal controls:
  - Play/Pause button
  - Timer slider
  - Info button

### 3. Visual Hierarchy
- Primary: Noise selection pills
- Secondary: Play/pause control
- Tertiary: Timer and info button

### 4. Color Usage
- Purposeful color coding for noise types:
  - Focus (White): #F5F5F5
  - Sleep (Brown): #D7CCC8
  - Create (Green): #C8E6C9
  - Study (Violet): #E1BEE7
  - Relax (Pink): #FFC1E3
  - Rest (Blue): #BBDEFB

### 5. Interaction Design
- One-tap operation where possible
- Use device volume controls
- No fancy animations or transitions
- Clear visual feedback for active states:
  - The currently playing noise pill must have a clear, functional indicator (e.g., a high-contrast border or a minimal play icon)
  - No decorative overlays or non-functional effects

### 6. Typography
- Clear, legible sans-serif fonts
- Minimal text usage
- Consistent text hierarchy

### 7. Privacy by Design
- No analytics
- No tracking
- No data collection
- No network calls

## User Flow
1. Open app
2. Select noise type
3. Hit play
4. (Optional) Adjust timer
5. Close app/turn off screen
