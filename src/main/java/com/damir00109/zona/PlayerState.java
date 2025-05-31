package com.damir00109.zona;

import java.util.Random;

public class PlayerState {
    private boolean outsideBorder;
    long timeOutsideStart;
    private boolean warned;
    boolean hasShownStrongNausea;
    private int consciousness;
    private boolean initializeRandomConsciousness; // This field might not be needed after initial setup
    private boolean diedDueToBorder = false;
    private long deathTick = 0; // Тик, на котором игрок должен умереть за границей
    private int currentDeathTimerDurationTicks = 0; // Длительность текущего таймера смерти в тиках
    private static final Random random = new Random();

    // New fields for warning states
    private boolean warnedAboutIntenseEffects = false;
    private boolean warnedAboutImminentDeath = false;

    private long lastTickTimeCheckedForSleep = -1; // Для механики бессонной ночи
    private boolean diedInEmergencyZone = false; // Новый флаг

    // Для зон восстановления ментального здоровья
    private double accumulatedRecovery = 0.0;

    // Новые поля для механики супа
    private boolean ateChamomileSoupRecently = false;
    private long timeWhenSoupWasEaten = 0; // Тик, когда суп был съеден
    private boolean chamomileDebuffActive = false;
    private long chamomileDebuffEndTick = 0;

    public PlayerState(boolean initializeRandom) {
        this.outsideBorder = false;
        this.timeOutsideStart = 0;
        this.warned = false;
        this.hasShownStrongNausea = false;
        this.initializeRandomConsciousness = initializeRandom;
        if (initializeRandom) {
            this.consciousness = 90 + random.nextInt(11); // 90-100
        } else {
            // This case is typically for loading existing data where consciousness is already set.
            // Defaulting to 100 if not initializing randomly and no specific value is loaded yet.
            this.consciousness = 100; 
        }
        this.diedDueToBorder = false;
        this.deathTick = 0;
        this.currentDeathTimerDurationTicks = 0;
        this.diedInEmergencyZone = false; // Инициализация нового флага
        this.accumulatedRecovery = 0.0; // Инициализация накопленного восстановления
        this.ateChamomileSoupRecently = false;
        this.timeWhenSoupWasEaten = 0;
        this.chamomileDebuffActive = false;
        this.chamomileDebuffEndTick = 0;
    }

    // Default constructor for cases like deserialization or when random init is the default
    public PlayerState() {
        this(true); // Initialize with random consciousness by default
    }

    public boolean isOutsideBorder() { return outsideBorder; }
    public void setOutsideBorder(boolean outsideBorder) { this.outsideBorder = outsideBorder; }
    public long getTimeOutsideStart() { return timeOutsideStart; }
    public void setTimeOutsideStart(long timeOutsideStart) { this.timeOutsideStart = timeOutsideStart; }
    public boolean hasBeenWarned() { return warned; }
    public void setWarned(boolean warned) { this.warned = warned; }
    public boolean hasShownStrongNausea() { return hasShownStrongNausea; }
    public void setHasShownStrongNausea(boolean hasShownStrongNausea) { this.hasShownStrongNausea = hasShownStrongNausea; }
    public int getConsciousness() { return consciousness; }
    public void setConsciousness(int consciousness) { this.consciousness = Math.max(0, Math.min(100, consciousness)); }
    public void decreaseConsciousness(int amount) { setConsciousness(this.consciousness - amount); }
    public boolean getDiedDueToBorder() { return diedDueToBorder; }
    public void setDiedDueToBorder(boolean diedDueToBorder) { this.diedDueToBorder = diedDueToBorder; }
    public long getDeathTick() { return deathTick; }
    public void setDeathTick(long deathTick) { this.deathTick = deathTick; }
    public int getCurrentDeathTimerDurationTicks() { return currentDeathTimerDurationTicks; }
    public void setCurrentDeathTimerDurationTicks(int currentDeathTimerDurationTicks) { this.currentDeathTimerDurationTicks = currentDeathTimerDurationTicks; }

    public boolean getDiedInEmergencyZone() { return diedInEmergencyZone; } // Геттер для нового флага
    public void setDiedInEmergencyZone(boolean diedInEmergencyZone) { this.diedInEmergencyZone = diedInEmergencyZone; } // Сеттер для нового флага

    // New methods
    public void resetBorderEffectsState() {
        this.outsideBorder = false;
        this.timeOutsideStart = 0;
        this.warned = false; // Assuming 'warned' is a general warning flag to be reset
        this.hasShownStrongNausea = false;
        this.warnedAboutIntenseEffects = false;
        this.warnedAboutImminentDeath = false;
        this.diedInEmergencyZone = false; // Сброс нового флага
        // this.deathTick = 0; // Should deathTick be reset here? Or only when returning to safe zone?
                           // Current logic in CustomBorderManager resets it when returning to safe zone.
        this.accumulatedRecovery = 0.0; // Сбрасываем при общем сбросе состояния эффектов
    }

    public boolean hasWarnedAboutIntenseEffects() {
        return warnedAboutIntenseEffects;
    }

    public void setWarnedAboutIntenseEffects(boolean warnedAboutIntenseEffects) {
        this.warnedAboutIntenseEffects = warnedAboutIntenseEffects;
    }

    public boolean hasWarnedAboutImminentDeath() {
        return warnedAboutImminentDeath;
    }

    public void setWarnedAboutImminentDeath(boolean warnedAboutImminentDeath) {
        this.warnedAboutImminentDeath = warnedAboutImminentDeath;
    }

    public long getLastTickTimeCheckedForSleep() {
        return lastTickTimeCheckedForSleep;
    }

    public void setLastTickTimeCheckedForSleep(long lastTickTimeCheckedForSleep) {
        this.lastTickTimeCheckedForSleep = lastTickTimeCheckedForSleep;
    }

    // Методы для управления накопленным восстановлением
    public double getAccumulatedRecovery() {
        return accumulatedRecovery;
    }

    public void setAccumulatedRecovery(double accumulatedRecovery) {
        this.accumulatedRecovery = accumulatedRecovery;
    }

    public void addAccumulatedRecovery(double value) {
        this.accumulatedRecovery += value;
        if (this.accumulatedRecovery >= 1.0) {
            int pointsToAdd = (int) this.accumulatedRecovery;
            setConsciousness(this.consciousness + pointsToAdd); // Используем сеттер для проверки границ
            this.accumulatedRecovery -= pointsToAdd; // Убираем целую часть
        }
    }

    // Геттеры и сеттеры для новых полей
    public boolean hasEatenChamomileSoupRecently() {
        return ateChamomileSoupRecently;
    }

    public void setAteChamomileSoupRecently(boolean ateChamomileSoupRecently) {
        this.ateChamomileSoupRecently = ateChamomileSoupRecently;
    }

    public long getTimeWhenSoupWasEaten() {
        return timeWhenSoupWasEaten;
    }

    public void setTimeWhenSoupWasEaten(long timeWhenSoupWasEaten) {
        this.timeWhenSoupWasEaten = timeWhenSoupWasEaten;
    }

    public boolean isChamomileDebuffActive() {
        return chamomileDebuffActive;
    }

    public void setChamomileDebuffActive(boolean chamomileDebuffActive) {
        this.chamomileDebuffActive = chamomileDebuffActive;
    }

    public long getChamomileDebuffEndTick() {
        return chamomileDebuffEndTick;
    }

    public void setChamomileDebuffEndTick(long chamomileDebuffEndTick) {
        this.chamomileDebuffEndTick = chamomileDebuffEndTick;
    }
} 