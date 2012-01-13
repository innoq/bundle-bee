/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bundlebee.bgbatch.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import org.hibernate.mapping.Set;

/**
 *
 * @author philipp
 */
@Entity
public class Planet implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToMany
    @JoinTable(name = "UserPlanets")
    private Set users;
    public Set getUsers(){
        return users;
    }
    private String identifier;
    private String type;
    private int xPos;
    private int yPos;
    private int zPos;
    private Long resourceMetal;
    private Long resourceBio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Planet)) {
            return false;
        }
        Planet other = (Planet) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "org.bundlebee.bgbatch.entities.Planet[id=" + id + "]";
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Long getResourceBio() {
        return resourceBio;
    }

    public void setResourceBio(Long resourceBio) {
        this.resourceBio = resourceBio;
    }

    public Long getResourceMetal() {
        return resourceMetal;
    }

    public void setResourceMetal(Long resourceMetal) {
        this.resourceMetal = resourceMetal;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getxPos() {
        return xPos;
    }

    public void setxPos(int xPos) {
        this.xPos = xPos;
    }

    public int getyPos() {
        return yPos;
    }

    public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    public int getzPos() {
        return zPos;
    }

    public void setzPos(int zPos) {
        this.zPos = zPos;
    }
}
