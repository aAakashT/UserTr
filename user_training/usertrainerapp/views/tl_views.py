from django.shortcuts import render, redirect
from usertrainerapp.serializers import UserSerializer, TrainingmoduleSerializer, ReviewSerializer
from rest_framework.views import APIView
from usertrainerapp.models import TrainingModule, Review , User
from usertrainerapp.permissions import IsTeamLead
from rest_framework.decorators import api_view, permission_classes 
from usertrainerapp.forms import AssignModuleForm
from rest_framework.renderers import TemplateHTMLRenderer
from rest_framework.response import Response
from django.core.mail import send_mail
from django.conf import settings
import json
@api_view(['GET'])
@permission_classes([IsTeamLead])
def api_get_users(request):
    if request.method == 'GET':
        user = request.user
        users = User.objects.filter(team_leader = user).prefetch_related('modules', 'reviews')
        serializer = UserSerializer(users, many=True)
        print(serializer.data)
        return Response({'data':serializer.data})
        # return render(request, 'tl_templates/User_list11.html', {'data': serializer.data})

def render_users(request):
    return render(request, 'tl_templates/User_list.html')

@api_view(['GET'])
@permission_classes([IsTeamLead])
def api_get_modules(request):
    
    if request.method == 'GET':
        t_modules = TrainingModule.objects.all()
        serializer = TrainingmoduleSerializer(t_modules, many=True)
        return Response( serializer.data)

def render_modules(request):
    return render(request, 'tl_templates/all_modules.html')

class AssignModules(APIView):
    permission_classes = [IsTeamLead,]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'tl_templates/select_module.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
            u_serializer = UserSerializer(user)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        training_modules = TrainingModule.objects.all()
        serializer = TrainingmoduleSerializer(training_modules, many=True)
        context = {'user': u_serializer.data,'training_modules': serializer.data}
        return Response(context)

    def post(self, request, user_id):
        user_id = user_id
        training_module_id = request.data.get('training_module')
        if not training_module_id:
            print(training_module)
            return Response({'error': 'Training module ID is required'}, status=400)
        try:
            user1 = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found'}, status=404)
        try:
            training_module = TrainingModule.objects.get(id=training_module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'Training module not found'}, status=404)
        try:
            training_module.user.add(user1)
        except Exception:
            return Response({'error': 'Could not add Training module'}, status=404)
        training_module.save()
        subject = "Training Module Assignment"
        message = f"You have been assigned the training module: {training_module.title}. Log in to the User Training Application to access it."
        email_from = settings.EMAIL_HOST_USER
        recipient_list = ['aakashthorve99@gmail.com', ]
        send_mail( subject, message, email_from, recipient_list )
        return Response({'success': 'Training module assigned'})

def assign_modules_UI(request):
    return render(request, 'tl_templates/select_module.html')    

def render_assign_page(request):
    return render(request, 'tl_templates/select_module.html')
