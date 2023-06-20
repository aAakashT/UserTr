from django import forms
from .models import User, TrainingModule, Review

class AssignModuleForm(forms.ModelForm):    
    class Meta:
        model = User
        fields = ['username', 'id']

    modules = forms.ModelMultipleChoiceField(queryset=TrainingModule.objects.all(), widget=forms.CheckboxSelectMultiple)    

    def assign_training(self):
        user = self.cleaned_data['user']
        modules = self.cleaned_data['modules']

class UserRoleTLForm(forms.ModelForm):
    class Meta:
        model = User
        fields = ['groups', 'team_leader']
        
    

# class TrainingModuleForm(forms.ModelForm):
#     class Meta:
#         model = TrainingModule
#         fields = [,]

#     def clean(self):
#         cleaned_data = super().clean()
#         asset_type = self.cleaned_data['asset_type']
#         return cleaned_data
    
# class ReviewForm(forms.ModelForm):
#     class Meta:
#         model = Review
#         fields = '__all__'

#     def clean(self):
#         cleaned_data = super().clean()
#         image = cleaned_data['image']
#         asset = cleaned_data['asset']
#         if image:
#             extension = image.name.split('.')[-1]
#             valid_extensions = ['jpg', 'jpeg', 'png', 'gif']
#             if extension.lower() not in valid_extensions:
#                 raise forms.ValidationError("Only JPG, JPEG, PNG, and GIF images are allowed.")

#             if image.size > 5 * 1024 * 1024: 
#                 raise forms.ValidationError("The image size should not exceed 5MB.")

#         return cleaned_data