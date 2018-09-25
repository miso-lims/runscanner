package ca.on.gsi.oicr.runscanner;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "SequencingParameters")

public class SequencingParametersDto implements Serializable
{

  private static final long serialVersionUID = 1L;

  @Enumerated(EnumType.STRING)
  private IlluminaChemistry chemistry;

  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private boolean paired;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long parametersId;

  @ManyToOne(targetEntity = PlatformModel.class)
  @JoinColumn(name = "platformId")
  private PlatformModel platform;
  @Column(nullable = false)
  private int readLength;

  public IlluminaChemistry getChemistry() {
    return chemistry;
  }

  public Long getId() {
    return parametersId;
  }

  public String getName() {
    return name;
  }

  public PlatformModel getPlatform() {
    return platform;
  }


  public int getReadLength() {
    return readLength;
  }

  public boolean isPaired() {
    return paired;
  }

  public void setChemistry(IlluminaChemistry chemistry) {
    this.chemistry = chemistry;
  }

  public void setId(Long id) {
    this.parametersId = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPaired(boolean paired) {
    this.paired = paired;
  }

  public void setPlatform(PlatformModel platform) {
    this.platform = platform;
  }

  public void setReadLength(int readLength) {
    this.readLength = readLength;
  }
}
