package br.ufpr.dinf.gres.domain.entity.metric;

import br.ufpr.dinf.gres.domain.entity.Execution;
import br.ufpr.dinf.gres.domain.entity.Experiment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "wocsinterface_metrics")
public class WocsinterfaceMetric extends BaseMetric {

    private static final long serialVersionUID = 1L;

    @Column(name = "wocsinterface")
    private Double wocsinterface;

    public WocsinterfaceMetric(String idSolution, Execution execution, Experiment experiement) {
        super(idSolution, execution, experiement);
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Double getWocsinterface() {
        return wocsinterface;
    }

    public void setWocsinterface(Double wocsinterface) {
        this.wocsinterface = wocsinterface;
    }


}
