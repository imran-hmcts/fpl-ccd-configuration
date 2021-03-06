const { I } = inject();

module.exports = {
  fields: {
    eightWeekExtensionOption: '#caseExtensionTimeList-EightWeekExtension',
    timetableForChildReason: '#caseExtensionReasonList-TimetableForChild',
    caseExtensionComment: '#extensionComments',
    caseExtensionTimeConfirmation: '#caseExtensionTimeConfirmationList-OtherExtension',
    caseExtensionDate: {
      day: '#eightWeeksExtensionDateOther-day',
      month: '#eightWeeksExtensionDateOther-month',
      year: '#eightWeeksExtensionDateOther-year',
    },
  },

  selectEightWeekExtensionTime() {
    I.click(this.fields.eightWeekExtensionOption);
  },

  selectTimetableForChildExtensionReason() {
    I.click(this.fields.timetableForChildReason);
  },

  addExtensionComment(comment) {
    I.fillField(this.fields.caseExtensionComment, comment);
  },

  addCaseExtensionTimeConfirmation(){
    I.click(this.fields.caseExtensionTimeConfirmation);
  },

  addCaseExtensionDate(){
    I.fillField(this.fields.caseExtensionDate.day, '10');
    I.fillField(this.fields.caseExtensionDate.month, '10');
    I.fillField(this.fields.caseExtensionDate.year, '2030');
  },
};
