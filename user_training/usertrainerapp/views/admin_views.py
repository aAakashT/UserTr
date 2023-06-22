from django.shortcuts import render, redirect
from usertrainerapp.serializers import TrainingmoduleSerializer, UserSerializer, UserUpdateSerializer
from usertrainerapp.models import TrainingModule, User 
from usertrainerapp.permissions import IsAdmin
from rest_framework.views import APIView
from rest_framework.response import Response
from rest_framework import status
from django.contrib.auth.models import Group
from rest_framework.response import Response
from django.db.models import Q
from rest_framework.renderers import TemplateHTMLRenderer
from django.contrib import messages


class TrainingCreateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_create.html'

    def get(self, request):
        try:
            Training = TrainingModule.objects.get(id=1)
        except TrainingModule.DoesNotExist:
            messages.error(request, 'User not found.')
            return Response({'error': 'User not found.'}, status=404)
        return Response({'Training': Training})

    def post(self, request):
        serializer = TrainingmoduleSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            messages.success(request, 'Training Created Sucessfully.')
            return redirect('training_modules')

            return Response(serializer.data, status=status.HTTP_201_CREATED)
        
        messages.error(request, 'Invalid data')
        return redirect('training_modules')
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class UserListView(APIView):
    permission_classes = [IsAdmin]
    def get(self, request):
        try:
            users = User.objects.filter(~Q(groups=1), is_active=True).prefetch_related('modules', 'reviews').exclude()
        except User.DoesNotExist:
            return Response({'error': "users does not exists"})
        serializer = UserSerializer(users, many=True)
        print(serializer.data)
        return Response({'data':serializer.data})

def show_users(request):
    return render(request, 'admin_templates/user_list.html')

class TrainingListView(APIView):
    permission_classes = [IsAdmin]
    def get(self, request):
        training_modules = TrainingModule.objects.all()
        serializer = TrainingmoduleSerializer(training_modules, many=True)
       
        return Response(serializer.data)
    # def get(self, request):
    #     training_modules = TrainingModule.objects.all()
    #     t_serializer = TrainingmoduleSerializer(training_modules, many=True)
    #     users = User.objects.all()
    #     u_serializer = UserSerializer(users, many=True)
    #     content = [t_serializer.data, u_serializer]
    #     return Response(content)

def show_training_modules(request):
    return render(request, 'admin_templates/module_list_1.html')

class AssignTLView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/assign_tl.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        tl_users = User.objects.filter(groups=2)

        context = {'user': user, 'tl_users': tl_users}
        return Response(context)

    def post(self, request, user_id):
        
        try:
            user = User.objects.get(id=user_id)
        except:
            messages.error(request, 'TL ID is required.')

            return Response({'error': 'TL ID is required.'}, status=400)
        # tl_group, created = Group.objects.get_or_create(name='Team_Leader')
        # if user.groups == 2 or user.groups == 1:
        #     return Response({'error': 'can only assign Tl to user group'}, status=400)

        tl_id = request.data.get('tl_id')
        print(tl_id)
        # print()
        if not tl_id:
            messages.error(request, 'TL ID is required.')
            
            return Response({'error': 'TL ID is required.'}, status=400)
        try:
            tl = User.objects.get(id=tl_id)
        except User.DoesNotExist:
            messages.error(request, 'TL ID is required.')

            return redirect('user_list')  
        user.team_leader = tl
        user.save()
        messages.success(request, f'{tl.username} is sucessfully assigned as TL of {user.username}')

        return redirect('show_users')


class AssignRoleView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/assign_role.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        return Response({'user': user})

    def post(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User is required.'}, status=400)
        tl_group= Group.objects.get(name='Team_Leader')
        # print(type(tl_group))
        if not tl_group:
            return Response({'error': 'Role is required.'}, status=400)
        if user.team_leader:
            messages.error(request, 'Cannot assign the TL role to a user who is already a team leader.')
            return redirect('show_users')
        user.groups.clear()
        user.groups.add(tl_group)
        messages.success(request, f'{user.username} is sucessfully assigned role')

        return redirect('show_users')

class UserDeleteView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/delete_user.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)

        context = {'user': user}
        return Response(context)

    def post(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        user.is_active = False
        user.save()
        messages.success(request, f'{user.username} is sucessfully deleted')

        return redirect('show_users')

class UserUpdateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/update_user.html'

    def get(self, request, user_id):
        try:
            user = User.objects.get(id=user_id)
        except User.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        try:
            team_leaders = User.objects.filter(~Q(id=user_id),~Q(groups__in=[1, 3]), is_active=True)
        except User.DoesNotExist:
            return Response({'error': 'TeamLeaders not found.'}, status=404)

        context = {'user': user, 'team_leaders': team_leaders}
        return Response(context)
    
    def post(self, request, user_id):
        # print(request.__dict__)
        email = request.data['email']
        username = request.data['username']
        team_lead_id = request.data['team_leader']
        try:
            user = User.objects.get(id=user_id)
            print(user.__dict__)
        except User.DoesNotExist:
            return Response({'error': 'User does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        user.email = email
        user.username = username
        if team_lead_id:
            try:
                team_leader = User.objects.get(id=team_lead_id)
            except User.DoesNotExist:
                return Response({'error': 'User does not exist.'}, status=status.HTTP_404_NOT_FOUND)
            user.team_leader = team_leader
        user.save()
        messages.success(request, f'{user.username} is sucessfully updated')

        return redirect('show_users')
            
class TrainingUpdateView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_update.html'
    
    def get(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        serializer = TrainingmoduleSerializer(training_module)
        context = {'module': training_module, 'serializer':serializer}
        return Response(context)

    def post(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'Training module does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        serializer = TrainingmoduleSerializer(training_module, data=request.data)
        if serializer.is_valid():
            serializer.save()
            messages.success(request, f'{training_module.title} is sucessfully updated')

            return redirect('training_modules')
        messages.error(request, 'invalid data')
        return redirect('training_modules')
        
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

class TrainingDeleteView(APIView):
    permission_classes = [IsAdmin]
    renderer_classes = [TemplateHTMLRenderer]
    template_name = 'admin_templates/training_delete.html'

    def get(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'User not found.'}, status=404)
        serializer = TrainingmoduleSerializer(training_module)
        context = {'module': training_module, 'serializer':serializer}
        return Response(context)


    def post(self, request, module_id):
        try:
            training_module = TrainingModule.objects.get(id=module_id)
        except TrainingModule.DoesNotExist:
            return Response({'error': 'Training module does not exist.'}, status=status.HTTP_404_NOT_FOUND)
        training_module.delete()
        messages.success(request, f'{training_module.title} is sucessfully deleted')

        return redirect('training_modules')


def admin_dashboard_view(request):
    return render(request, 'admin_templates/admin_dashboard.html')