import {Component, Inject, OnInit} from "@angular/core";
import {FormBuilder} from "@angular/forms";
import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {ActivatedRoute, Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";
import {Location} from "@angular/common";
import {OptimizationService} from "../../services/optimization.service";
import {OptimizationInfo} from "../../dto/optimization-info";

@Component({
  selector: 'interaction-dialog',
  templateUrl: 'interaction-dialog.component.html',
})
export class InteractionDialogComponent implements OnInit {
  solutionSet: any = {};
  solutions = [];
  info: OptimizationInfo;
  clusters: number[] = [];
  selff: any;

  constructor(
    public dialogRef: MatDialogRef<InteractionDialogComponent>, public router: Router, public route: ActivatedRoute,
    private fb: FormBuilder, public snackBar: MatSnackBar, public location: Location,
    public optimizationService: OptimizationService,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.selff = this;
  }


  ngOnInit(): void {
    this.solutionSet = this.data.interaction.solutionSet;
    this.solutions = this.solutionSet.solutions;
    this.info = this.data.info;
    for (let obj of Object.keys(this.solutionSet.clusterIds)) {
      this.clusters.push(Math.round(Number(obj)));
    }
    this.clusters = this.clusters.sort();
    console.log("clusters", this.clusters)
  }

  filter(clusterId) {
    return this.solutions.filter(solution => solution.clusterId === clusterId);
  }

  close(element: any) {
    this.dialogRef.close(element)
  }

  download(id?) {
    this.optimizationService.downloadOneAlternative(this.info.hash, id).subscribe(result => {
      this.snackBar.open("Your download is available", null, {
        duration: 2000
      });
      const blob = new Blob([result], {
        type: 'application/zip'
      });
      const url = window.URL.createObjectURL(blob);
      window.open(url);
    })
  }

  downloadAll() {
    this.optimizationService.downloadAllAlternative(this.info.hash).subscribe(result => {
      this.snackBar.open("Your download is available", null, {
        duration: 2000
      });
      const blob = new Blob([result], {
        type: 'application/zip'
      });
      const url = window.URL.createObjectURL(blob);
      window.open(url);
    })
  }

  open(id?) {
    this.snackBar.open("Your PLA is opening, wait a minute", "OK", {
      duration: 5000
    });
    this.optimizationService.openOneAlternative(this.info.hash, id).subscribe(result => {
    })
  }

  getNumber(value: any) {
    return Number(value);
  }

  onFileSelected(solution, index) {
    let element = document.getElementById('file-' + index);
    let files = <FileList>element['files'];
    console.log(solution, index, files)
    this.optimizationService.postArchitecturalInteraction(files, this.info.hash, index)
      .subscribe(res => {
        this.snackBar.open("The file was uploaded", null, {
          duration: 2000
        })
      })
  }

  selectPLA(index) {
    document.getElementById('file-' + index).click()
  }
}
