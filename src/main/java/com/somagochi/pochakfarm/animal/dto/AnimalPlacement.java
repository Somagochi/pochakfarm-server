package com.somagochi.pochakfarm.animal.dto;

public record AnimalPlacement(Long spaceId, int floorNum, int slotNum) {

  public boolean isAt(int floorNum, int slotNum) {
    return this.floorNum == floorNum && this.slotNum == slotNum;
  }
}
